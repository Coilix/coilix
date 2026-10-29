package io.github.coilix.context

import io.github.coilix.annotation.Bean
import io.github.coilix.annotation.Configuration
import io.github.coilix.annotation.Primary
import java.lang.reflect.Method

final class ReflectiveBeanMethodScanner(parameterMapper: ParameterDependencyMapper) extends BeanMethodScanner {

  override def scan(owner: Class[?]): List[BeanDefinition] =
    if (owner.isAnnotationPresent(classOf[Configuration])) beanMethods(owner).map(definitionOf(owner))
    else Nil

  private def beanMethods(owner: Class[?]): List[Method] =
    owner.getMethods.toList.filter(_.isAnnotationPresent(classOf[Bean])).sortBy(_.getName)

  private def definitionOf(owner: Class[?])(method: Method): BeanDefinition =
    BeanDefinition(
      beanName     = beanNameOf(method),
      beanClass    = method.getReturnType,
      factory      = MethodInstanceFactory(method),
      dependencies = Dependency.Required(owner) :: method.getParameters.toList.map(parameterMapper.map),
      isPrimary    = method.isAnnotationPresent(classOf[Primary])
    )

  private def beanNameOf(method: Method): String =
    Option(method.getAnnotation(classOf[Bean]).value).filter(_.nonEmpty).getOrElse(method.getName)
}
