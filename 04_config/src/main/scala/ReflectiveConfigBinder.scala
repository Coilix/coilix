package dev.coilix.config

import cats.syntax.all.*
import dev.coilix.annotation.Autowired
import dev.coilix.common.CoilixException
import dev.coilix.config.enums.EnumResolver
import dev.coilix.config.valuetype.ValueTypeResolver
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import scala.util.Try

/**
 * Binds configuration values to objects through reflection.
 *
 * The target type decides how the value is read:
 *
 *  - `Option[T]`: `None` when the path is not set, otherwise the bound `T`,
 *  - `Map[String, V]`: one entry per key of the section,
 *  - `List[T]`, `Vector[T]` and `Set[T]` (and their supertypes such as `Seq[T]`): one element per list item,
 *  - a class annotated with [[dev.coilix.annotation.ValueType]]: the inner value, bound by these rules and wrapped in the
 *    class,
 *  - an enum annotated with [[dev.coilix.annotation.Enumerated]]: the case with the name given as text,
 *  - a type supported by the [[dev.coilix.config.ScalarReader]]: the scalar value,
 *  - `AnyRef`: the plain value from [[dev.coilix.config.ConfigTree.anyRef]],
 *  - any other class: a new instance created from the section.
 *
 * To create an instance, the binder uses the only public constructor, or the one annotated with `@Autowired` when there are
 * several. Each parameter is read from the key with the same name or its kebab-case form, and falls back to its Scala
 * default argument. A section with a `class` key creates that class instead, which must extend the target type; this is how
 * replaceable components are configured, and it is required when the target type is a trait or an abstract class.
 *
 * ```yaml
 * shop:
 *   mail:
 *     host: smtp.example.com
 *     connect-timeout: 5s
 * ```
 *
 * ```scala
 * final case class MailSettings(host: String, port: Int = 25, connectTimeout: FiniteDuration)
 * ```
 *
 * Every failure is returned as a [[dev.coilix.config.ConfigError]] that names the path. An exception thrown by the
 * constructor of a bound class becomes a [[dev.coilix.config.ConfigError.CreationFailed]], and a value rejected by a value
 * type or an enum an [[dev.coilix.config.ConfigError.InvalidValue]].
 *
 * @param scalars
 *   reader for single values
 * @param defaults
 *   source of default arguments for constructor parameters
 * @param classLoader
 *   class loader for classes named by `class` keys
 * @param valueTypes
 *   recognizes and creates value types
 * @param enums
 *   recognizes enums and finds their cases by name
 */
case class ReflectiveConfigBinder(
  scalars:     ScalarReader,
  defaults:    DefaultArguments,
  classLoader: ClassLoader,
  valueTypes:  ValueTypeResolver = ValueTypeResolver.default,
  enums:       EnumResolver      = EnumResolver.default
) extends ConfigBinder {

  private type Bound[A] = Either[ConfigError, A]

  private val classKey = "class"

  override def bind(tree: ConfigTree, path: String, targetType: Type): Bound[AnyRef] =
    targetType match {
      case parameterized: ParameterizedType => bindGeneric(tree, path, rawClassOf(parameterized), typeArguments(parameterized))
      case targetClass:   Class[?]          => bindGeneric(tree, path, targetClass, Nil)
      case _ => bindGeneric(tree, path, classOf[AnyRef], Nil)
    }

  private def bindGeneric(tree: ConfigTree, path: String, raw: Class[?], arguments: List[Type]): Bound[AnyRef] =
    if (raw == classOf[Option[?]]) bindOption(tree, path, argument(arguments, 0))
    else if (!tree.contains(path)) Left(ConfigError.Missing(path, raw))
    else if (classOf[Map[?, ?]].isAssignableFrom(raw)) bindMap(tree, path, argument(arguments, 1))
    else if (classOf[Iterable[?]].isAssignableFrom(raw)) bindCollection(tree, path, raw, argument(arguments, 0))
    else bindClass(tree, path, raw)

  private def bindClass(tree: ConfigTree, path: String, target: Class[?]): Bound[AnyRef] =
    if (valueTypes.isValueType(target)) bindValueType(tree, path, target)
    else if (enums.isEnum(target)) bindEnum(tree, path, target)
    else if (hasClassKey(tree, path)) configuredClass(tree, path, target).flatMap(construct(tree, path, _))
    else if (scalars.supports(target)) scalars.read(tree, path, target)
    else if (target == classOf[AnyRef]) tree.anyRef(path)
    else if (tree.isSection(path)) construct(tree, path, target)
    else Left(ConfigError.UnsupportedTarget(path, target))

  private def bindValueType(tree: ConfigTree, path: String, target: Class[?]): Bound[AnyRef] =
    for {
      definition <- checked(path, target)(valueTypes.resolve(target))
      inner      <- bind(tree, path, definition.innerType)
      value      <- checked(path, target)(definition.create(inner))
    } yield value

  private def bindEnum(tree: ConfigTree, path: String, target: Class[?]): Bound[AnyRef] =
    for {
      definition <- checked(path, target)(enums.resolve(target))
      name       <- tree.string(path)
      value      <- checked(path, target)(definition.valueOf(name))
    } yield value

  private def checked[A](path: String, target: Class[?])(conversion: => A): Bound[A] =
    try {
      Right(conversion)
    } catch {
      case error: CoilixException => Left(ConfigError.InvalidValue(path, target, error.getMessage))
    }

  private def bindOption(tree: ConfigTree, path: String, element: Type): Bound[AnyRef] =
    if (tree.contains(path)) bind(tree, path, element).map(Some(_))
    else Right(None)

  private def bindMap(tree: ConfigTree, path: String, valueType: Type): Bound[AnyRef] =
    tree.keys(path).flatMap(_.traverse(key => bind(tree, tree.child(path, key), valueType).tupleLeft(key))).map(_.toMap)

  private def bindCollection(tree: ConfigTree, path: String, raw: Class[?], element: Type): Bound[AnyRef] =
    tree.elements(path).flatMap(_.traverse(bind(_, ConfigTree.elementKey, element))).flatMap(collectionOf(path, raw, _))

  private def collectionOf(path: String, raw: Class[?], values: List[AnyRef]): Bound[AnyRef] =
    if (raw.isAssignableFrom(classOf[List[?]])) Right(values)
    else if (raw.isAssignableFrom(classOf[Vector[?]])) Right(values.toVector)
    else if (raw.isAssignableFrom(classOf[Set[?]])) Right(values.toSet)
    else Left(ConfigError.UnsupportedCollection(path, raw))

  private def hasClassKey(tree: ConfigTree, path: String): Boolean =
    tree.isSection(path) && tree.contains(tree.child(path, classKey))

  private def configuredClass(tree: ConfigTree, path: String, expected: Class[?]): Bound[Class[?]] =
    tree.string(tree.child(path, classKey)).flatMap(loadClass(path, _)).flatMap(assignable(path, expected, _))

  private def loadClass(path: String, className: String): Bound[Class[?]] =
    Try(Class.forName(className, false, classLoader)).toOption.toRight(ConfigError.UnknownClass(path, className))

  private def assignable(path: String, expected: Class[?], actual: Class[?]): Bound[Class[?]] =
    Either.cond(expected.isAssignableFrom(actual), actual, ConfigError.IncompatibleClass(path, actual, expected))

  private def construct(tree: ConfigTree, path: String, target: Class[?]): Bound[AnyRef] =
    for {
      constructor <- constructorOf(path, target)
      arguments   <- constructor.getParameters.toList.zipWithIndex.traverse(argumentFor(tree, path, target))
      instance    <- instantiate(path, constructor, arguments)
    } yield instance

  private def constructorOf(path: String, target: Class[?]): Bound[Constructor[?]] =
    if (target.isInterface || Modifier.isAbstract(target.getModifiers)) Left(ConfigError.AbstractTarget(path, target))
    else selectConstructor(path, target)

  private def selectConstructor(path: String, target: Class[?]): Bound[Constructor[?]] =
    target.getConstructors.toList match {
      case single :: Nil => Right(single)
      case many          => autowired(many).toRight(ConfigError.AmbiguousConstructor(path, target))
    }

  private def autowired(constructors: List[Constructor[?]]): Option[Constructor[?]] =
    constructors.find(_.isAnnotationPresent(classOf[Autowired]))

  private def argumentFor(tree: ConfigTree, path: String, owner: Class[?])(parameter: Parameter, index: Int): Bound[AnyRef] =
    parameterPath(tree, path, parameter.getName)
      .map(found => bind(tree, found, parameter.getParameterizedType))
      .orElse(defaults.find(owner, index).map(Right(_)))
      .getOrElse(bind(tree, tree.child(path, PropertyNames.kebabCase(parameter.getName)), parameter.getParameterizedType))

  private def parameterPath(tree: ConfigTree, path: String, name: String): Option[String] =
    PropertyNames.candidates(name).map(tree.child(path, _)).find(tree.contains)

  private def instantiate(path: String, constructor: Constructor[?], arguments: List[AnyRef]): Bound[AnyRef] =
    try {
      Right(constructor.newInstance(arguments*))
    } catch {
      case error: InvocationTargetException =>
        Left(ConfigError.CreationFailed(path, constructor.getDeclaringClass, error.getCause))
    }

  private def rawClassOf(parameterized: ParameterizedType): Class[?] =
    parameterized.getRawType match {
      case raw: Class[?] => raw
      case _ => classOf[AnyRef]
    }

  private def typeArguments(parameterized: ParameterizedType): List[Type] =
    parameterized.getActualTypeArguments.toList

  private def argument(arguments: List[Type], index: Int): Type =
    arguments.lift(index).getOrElse(classOf[AnyRef])
}
