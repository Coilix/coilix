package dev.coilix.config

import dev.coilix.annotation.Autowired
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
 * Every failure is reported as a [[dev.coilix.config.ConfigurationException]] that names the path.
 *
 * @param scalars
 *   reader for single values
 * @param defaults
 *   source of default arguments for constructor parameters
 * @param classLoader
 *   class loader for classes named by `class` keys
 */
final class ReflectiveConfigBinder(scalars: ScalarReader, defaults: DefaultArguments, classLoader: ClassLoader)
    extends ConfigBinder {

  private val classKey = "class"

  override def bind(tree: ConfigTree, path: String, targetType: Type): AnyRef =
    targetType match {
      case parameterized: ParameterizedType => bindGeneric(tree, path, rawClassOf(parameterized), typeArguments(parameterized))
      case targetClass:   Class[?]          => bindGeneric(tree, path, targetClass, Nil)
      case _ => bindGeneric(tree, path, classOf[AnyRef], Nil)
    }

  private def bindGeneric(tree: ConfigTree, path: String, raw: Class[?], arguments: List[Type]): AnyRef =
    if (raw == classOf[Option[?]]) bindOption(tree, path, argument(arguments, 0))
    else if (!tree.contains(path)) throw missing(path, raw)
    else if (classOf[Map[?, ?]].isAssignableFrom(raw)) bindMap(tree, path, argument(arguments, 1))
    else if (classOf[Iterable[?]].isAssignableFrom(raw)) bindCollection(tree, path, raw, argument(arguments, 0))
    else bindClass(tree, path, raw)

  private def bindClass(tree: ConfigTree, path: String, target: Class[?]): AnyRef =
    if (hasClassKey(tree, path)) construct(tree, path, configuredClass(tree, path, target))
    else if (scalars.supports(target)) scalars.read(tree, path, target)
    else if (target == classOf[AnyRef]) tree.anyRef(path)
    else if (tree.isSection(path)) construct(tree, path, target)
    else throw ConfigurationException(s"Cannot bind configuration '$path' to ${target.getName}")

  private def bindOption(tree: ConfigTree, path: String, element: Type): Option[AnyRef] =
    Option.when(tree.contains(path))(bind(tree, path, element))

  private def bindMap(tree: ConfigTree, path: String, valueType: Type): Map[String, AnyRef] =
    tree.keys(path).map(key => key -> bind(tree, tree.child(path, key), valueType)).toMap

  private def bindCollection(tree: ConfigTree, path: String, raw: Class[?], element: Type): AnyRef =
    collectionOf(path, raw, tree.elements(path).map(bind(_, ConfigTree.elementKey, element)))

  private def collectionOf(path: String, raw: Class[?], values: List[AnyRef]): AnyRef =
    if (raw.isAssignableFrom(classOf[List[?]])) values
    else if (raw.isAssignableFrom(classOf[Vector[?]])) values.toVector
    else if (raw.isAssignableFrom(classOf[Set[?]])) values.toSet
    else throw ConfigurationException(s"Cannot bind configuration '$path' to collection ${raw.getName}")

  private def hasClassKey(tree: ConfigTree, path: String): Boolean =
    tree.isSection(path) && tree.contains(tree.child(path, classKey))

  private def configuredClass(tree: ConfigTree, path: String, expected: Class[?]): Class[?] =
    assignable(path, expected, loadClass(path, tree.string(tree.child(path, classKey))))

  private def loadClass(path: String, className: String): Class[?] =
    Try(Class.forName(className, false, classLoader))
      .getOrElse(throw ConfigurationException(s"Class '$className' configured at '$path.$classKey' cannot be loaded"))

  private def assignable(path: String, expected: Class[?], actual: Class[?]): Class[?] =
    if (expected.isAssignableFrom(actual)) actual
    else throw ConfigurationException(s"Class '${actual.getName}' configured at '$path.$classKey' is not a ${expected.getName}")

  private def construct(tree: ConfigTree, path: String, target: Class[?]): AnyRef = {
    val constructor = constructorOf(path, target)
    instantiate(path, constructor, constructor.getParameters.toList.zipWithIndex.map(argumentFor(tree, path, target)))
  }

  private def constructorOf(path: String, target: Class[?]): Constructor[?] =
    if (target.isInterface || Modifier.isAbstract(target.getModifiers)) throw abstractTarget(path, target)
    else selectConstructor(path, target)

  private def selectConstructor(path: String, target: Class[?]): Constructor[?] =
    target.getConstructors.toList match {
      case single :: Nil => single
      case many          => many.find(_.isAnnotationPresent(classOf[Autowired])).getOrElse(throw ambiguousConstructor(path, target))
    }

  private def argumentFor(tree: ConfigTree, path: String, owner: Class[?])(parameter: Parameter, index: Int): AnyRef =
    parameterPath(tree, path, parameter.getName)
      .map(found => bind(tree, found, parameter.getParameterizedType))
      .orElse(defaults.find(owner, index))
      .getOrElse(bind(tree, tree.child(path, PropertyNames.kebabCase(parameter.getName)), parameter.getParameterizedType))

  private def parameterPath(tree: ConfigTree, path: String, name: String): Option[String] =
    PropertyNames.candidates(name).map(tree.child(path, _)).find(tree.contains)

  private def instantiate(path: String, constructor: Constructor[?], arguments: List[AnyRef]): AnyRef =
    try {
      constructor.newInstance(arguments*)
    } catch {
      case error: InvocationTargetException => throw creationFailed(path, constructor.getDeclaringClass, error.getCause)
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

  private def missing(path: String, target: Class[?]): ConfigurationException =
    ConfigurationException(s"Missing configuration '$path' (expected ${target.getName})")

  private def abstractTarget(path: String, target: Class[?]): ConfigurationException =
    ConfigurationException(
      s"Configuration '$path' binds to abstract type ${target.getName}, set '$path.$classKey' to a concrete class"
    )

  private def ambiguousConstructor(path: String, target: Class[?]): ConfigurationException =
    ConfigurationException(s"${target.getName} bound at '$path' needs one public constructor or one marked @Autowired")

  private def creationFailed(path: String, target: Class[?], cause: Throwable): ConfigurationException =
    ConfigurationException(s"Cannot create ${target.getName} from configuration '$path': $cause")
}
