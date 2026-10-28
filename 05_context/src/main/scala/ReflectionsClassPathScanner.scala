package org.scaling.context

import java.lang.annotation.Annotation
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import org.scaling.annotation.Component
import org.scaling.annotation.Configuration
import org.scaling.annotation.ConfigurationProperties
import org.scaling.annotation.Repository
import org.scaling.annotation.Service
import scala.jdk.CollectionConverters.*

final class ReflectionsClassPathScanner(factory: BeanDefinitionFactory) extends ClassPathScanner {

  private val componentAnnotations: List[Class[? <: Annotation]] =
    List(classOf[Component], classOf[Service], classOf[Repository], classOf[Configuration], classOf[ConfigurationProperties])

  override def scan(basePackage: String): List[BeanDefinition] =
    componentTypes(reflectionsFor(basePackage)).flatMap(factory.create)

  private def reflectionsFor(basePackage: String): Reflections =
    Reflections(
      ConfigurationBuilder()
        .forPackage(basePackage)
        .filterInputsBy(FilterBuilder().includePackage(basePackage))
        .setScanners(Scanners.TypesAnnotated)
    )

  private def componentTypes(reflections: Reflections): List[Class[?]] =
    componentAnnotations.flatMap(annotatedWith(reflections)).distinct.sortBy(_.getName)

  private def annotatedWith(reflections: Reflections)(annotation: Class[? <: Annotation]): List[Class[?]] =
    reflections.getTypesAnnotatedWith(annotation).asScala.toList
}
