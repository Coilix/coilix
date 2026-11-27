package dev.coilix.context

import dev.coilix.annotation.Component
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.ConfigurationProperties
import dev.coilix.annotation.Repository
import dev.coilix.annotation.Service
import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import scala.jdk.CollectionConverters.*

/**
 * Finds classes annotated with `@Component`, `@Service`, `@Repository`, `@Configuration` or `@ConfigurationProperties`
 * using the Reflections library.
 *
 * Classes are processed in order of their fully qualified names, so the result does not depend on the class path order.
 * This is the default [[dev.coilix.context.ClassPathScanner]].
 *
 * @param factory
 *   creates the definitions for each class found
 */
case class ReflectionsClassPathScanner(factory: BeanDefinitionFactory) extends ClassPathScanner {

  private val componentAnnotations: List[Class[? <: Annotation]] =
    List(classOf[Component], classOf[Service], classOf[Repository], classOf[Configuration], classOf[ConfigurationProperties])

  override def scan(basePackage: String): List[BeanDefinition] =
    componentTypes(reflectionsFor(basePackage)).flatMap(factory.create)

  override def scanAnnotated(basePackage: String, annotations: List[Class[? <: Annotation]]): List[Class[?]] =
    if (annotations.isEmpty) Nil else typesAnnotatedWith(reflectionsFor(basePackage), annotations).filter(isConcrete)

  private def reflectionsFor(basePackage: String): Reflections =
    Reflections(
      ConfigurationBuilder()
        .forPackage(basePackage)
        .filterInputsBy(FilterBuilder().includePackage(basePackage))
        .setScanners(Scanners.TypesAnnotated)
    )

  private def componentTypes(reflections: Reflections): List[Class[?]] =
    typesAnnotatedWith(reflections, componentAnnotations)

  private def typesAnnotatedWith(reflections: Reflections, annotations: List[Class[? <: Annotation]]): List[Class[?]] =
    annotations.flatMap(annotatedWith(reflections)).distinct.sortBy(_.getName)

  private def isConcrete(candidate: Class[?]): Boolean =
    !candidate.isInterface && !candidate.isAnnotation && !Modifier.isAbstract(candidate.getModifiers)

  private def annotatedWith(reflections: Reflections)(annotation: Class[? <: Annotation]): List[Class[?]] =
    reflections.getTypesAnnotatedWith(annotation).asScala.toList
}
