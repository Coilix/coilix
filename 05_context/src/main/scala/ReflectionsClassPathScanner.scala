package io.github.coilix.context

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Configuration
import io.github.coilix.annotation.ConfigurationProperties
import io.github.coilix.annotation.Repository
import io.github.coilix.annotation.Service
import java.lang.annotation.Annotation
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
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
