package dev.coilix.context

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Primary
import dev.coilix.common.GenericType
import java.lang.reflect.Method

/**
 * Finds the public `@Bean` methods of a `@Configuration` class.
 *
 * Each method gives a bean named after the `@Bean` value or the method name, registered under the method's return type and
 * depending on the configuration bean and the method's parameters. Methods are processed by name. Classes without
 * `@Configuration` give no definitions. A return type recognized as an effect, such as `IO[A]`, gives a bean of type `A` with
 * the matching [[dev.coilix.context.BeanResult]]. This is the default [[dev.coilix.context.BeanMethodScanner]].
 *
 * @param parameterMapper
 *   classifies method parameters
 * @param recognizer
 *   finds return types that are effects
 */
final class ReflectiveBeanMethodScanner(parameterMapper: ParameterDependencyMapper, recognizer: EffectfulResultRecognizer)
    extends BeanMethodScanner {

  override def scan(owner: Class[?]): List[BeanDefinition] =
    if (owner.isAnnotationPresent(classOf[Configuration])) beanMethods(owner).map(definitionOf(owner))
    else Nil

  private def beanMethods(owner: Class[?]): List[Method] =
    owner.getMethods.toList.filter(_.isAnnotationPresent(classOf[Bean])).sortBy(_.getName)

  private def definitionOf(owner: Class[?])(method: Method): BeanDefinition = {
    val returnType     = GenericType.of(method.getGenericReturnType)
    val result         = recognizer.recognize(returnType).getOrElse(BeanResult.Plain)
    BeanDefinition(
      beanName     = beanNameOf(method),
      beanType     = result.beanType(returnType),
      factory      = MethodInstanceFactory(method),
      dependencies = Dependency.Required(GenericType.of(owner)) :: method.getParameters.toList.map(parameterMapper.map),
      isPrimary    = method.isAnnotationPresent(classOf[Primary]),
      result       = result
    )
  }

  private def beanNameOf(method: Method): String =
    Option(method.getAnnotation(classOf[Bean]).value).filter(_.nonEmpty).getOrElse(method.getName)
}
