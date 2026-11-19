package dev.coilix.context

import dev.coilix.annotation.Value
import dev.coilix.common.GenericType
import dev.coilix.config.ValueExpression
import java.lang.reflect.Parameter

/**
 * Maps a parameter annotated with `@Value` to a [[dev.coilix.context.Dependency.Value]], and any other parameter by its type with
 * the
 * [[dev.coilix.context.DependencyTypeMapper]].
 *
 * A bean type must be fully known: a type variable that the declaring class or method leaves unbound, such as `F` in
 * `SqlRepository[F[_]](sync: Sync[F])`, and a type argument that is a class with several type parameters, which is how the JVM
 * keeps a type lambda such as `[A] =>> Either[String, A]`, are rejected with
 * [[dev.coilix.context.UnsupportedDependencyTypeException]]. This is the default
 * [[dev.coilix.context.ParameterDependencyMapper]].
 */
final class AnnotatedParameterDependencyMapper(typeMapper: DependencyTypeMapper) extends ParameterDependencyMapper {

  override def map(parameter: Parameter): Dependency =
    Option(parameter.getAnnotation(classOf[Value]))
      .map(value => Dependency.Value(ValueExpression.parse(value.value), parameter.getParameterizedType))
      .getOrElse(checked(parameter, typeMapper.map(parameter.getParameterizedType)))

  private def checked(parameter: Parameter, dependency: Dependency): Dependency = {
    beanTypeOf(dependency).foreach(beanType => check(parameter, beanType))
    dependency
  }

  private def beanTypeOf(dependency: Dependency): Option[GenericType] =
    dependency match {
      case Dependency.Required(beanType)   => Some(beanType)
      case Dependency.Optional(beanType)   => Some(beanType)
      case Dependency.Collection(beanType) => Some(beanType)
      case Dependency.Value(_, _)          => None
    }

  private def check(parameter: Parameter, beanType: GenericType): Unit = {
    beanType.variables.headOption.foreach(variable => throw unbound(parameter, beanType, variable))
    lossyArgument(beanType).foreach(argument       => throw lossy(parameter, beanType, argument))
  }

  private def lossyArgument(beanType: GenericType): Option[Class[?]] =
    beanType match {
      case GenericType.Concrete(_, arguments) =>
        arguments.flatMap(rawConstructor).headOption.orElse(arguments.flatMap(lossyArgument).headOption)
      case _                                  => None
    }

  private def rawConstructor(argument: GenericType): Option[Class[?]] =
    argument match {
      case GenericType.Concrete(raw, Nil) if raw.getTypeParameters.length > 1 => Some(raw)
      case _                                                                  => None
    }

  private def unbound(parameter: Parameter, beanType: GenericType, variable: String): UnsupportedDependencyTypeException =
    UnsupportedDependencyTypeException(
      s"${describe(parameter)} has type $beanType with the unbound type variable $variable. " +
        "Declare the bean through a @Bean method with a concrete type instead"
    )

  private def lossy(parameter: Parameter, beanType: GenericType, argument: Class[?]): UnsupportedDependencyTypeException =
    UnsupportedDependencyTypeException(
      s"${describe(parameter)} has type $beanType, whose argument ${argument.getName} takes ${argument.getTypeParameters.length} " +
        "type parameters. It is probably a type lambda or a partially applied type, which the JVM keeps only as its class. " +
        "Declare a named class for it"
    )

  private def describe(parameter: Parameter): String =
    s"Parameter ${parameter.getName} of ${parameter.getDeclaringExecutable.getDeclaringClass.getName}.${executableName(parameter)}"

  private def executableName(parameter: Parameter): String =
    parameter.getDeclaringExecutable match {
      case _: java.lang.reflect.Constructor[?] => "<init>"
      case method => method.getName
    }
}
