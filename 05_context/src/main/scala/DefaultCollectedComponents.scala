package dev.coilix.context

import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.common.TypeInfo
import java.lang.annotation.Annotation
import scala.reflect.ClassTag

/**
 * Creates collected classes from their definitions and from dependency values resolved when the context was built.
 *
 * The values are resolved once, so every instance of a collected class receives the same beans. Each new instance goes through
 * the [[dev.coilix.context.BeanPostProcessor]] before it is returned. This is the implementation created by
 * [[dev.coilix.context.DefaultComponentCollector]].
 *
 * @param definitions
 *   the definitions of the collected classes
 * @param arguments
 *   the value of each dependency of the collected classes
 * @param postProcessor
 *   initializes each new instance
 * @param destroyer
 *   runs the `@PreDestroy` methods when an instance is closed
 */
case class DefaultCollectedComponents(
  definitions:   List[BeanDefinition],
  arguments:     Map[Dependency, AnyRef],
  postProcessor: BeanPostProcessor,
  destroyer:     BeanDestroyer
) extends CollectedComponents {

  override val classes: List[Class[?]] = definitions.map(_.beanClass).sortBy(_.getName)

  override def annotatedWith[A <: Annotation: ClassTag]: List[Class[?]] =
    classes.filter(_.isAnnotationPresent(runtimeTypeOf[A]))

  override def create[T: TypeInfo](componentClass: Class[?]): ManagedComponent[T] =
    managed[T](assignable[T](definitionOf(componentClass)))

  private def definitionOf(componentClass: Class[?]): BeanDefinition =
    definitions.find(_.beanClass == componentClass).getOrElse(throw notCollected(componentClass))

  private def assignable[T: TypeInfo](definition: BeanDefinition): BeanDefinition =
    if (TypeInfo[T].genericType.isAssignableFrom(definition.beanType)) definition else throw notOfType[T](definition)

  private def managed[T: TypeInfo](definition: BeanDefinition): ManagedComponent[T] = {
    val instance = postProcessor.postProcess(definition, definition.factory.create(definition.dependencies.map(arguments)))
    ManagedComponent(TypeInfo[T].runtimeClass.cast(instance), registered(definition, instance), destroyer)
  }

  private def registered(definition: BeanDefinition, instance: AnyRef): RegisteredBean =
    RegisteredBean(definition.beanName, definition.beanType, isPrimary = false, instance, definition.lifecycle)

  private def notCollected(componentClass: Class[?]): BeanCreationException =
    BeanCreationException(s"Class ${componentClass.getName} is not a collected component")

  private def notOfType[T: TypeInfo](definition: BeanDefinition): BeanCreationException =
    BeanCreationException(s"Collected component ${definition.beanClass.getName} is not a ${TypeInfo[T].genericType}")
}
