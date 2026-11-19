package dev.coilix.context

import dev.coilix.common.TypeInfo
import java.lang.annotation.Annotation
import scala.reflect.ClassTag

/**
 * The classes collected for modules instead of becoming beans, and the way to create them.
 *
 * The context registers it as the bean `collectedComponents` when an enabled module lists annotations in
 * [[dev.coilix.context.CoilixModule.collects]]. A collected class gets its dependencies, `@Value` parameters and
 * `@PostConstruct` methods like a bean, but no bean can inject it, so only the module decides when it is created and how it is
 * used. The module bean creates the instances and closes them when the context closes:
 *
 * ```scala
 * @Component
 * final class JobRunner(components: CollectedComponents) {
 *   private val jobs = components.annotatedWith[Job].map(components.create[Runnable])
 *
 *   def runAll(): Unit = jobs.foreach(_.instance.run())
 *
 *   @PreDestroy
 *   def stop(): Unit = jobs.foreach(_.close())
 * }
 * ```
 */
trait CollectedComponents {

  /** All collected classes, sorted by their fully qualified names. */
  def classes: List[Class[?]]

  /** The collected classes marked with `A`, sorted by their fully qualified names. */
  def annotatedWith[A <: Annotation: ClassTag]: List[Class[?]]

  /**
   * Creates a new instance of `componentClass`, with its dependencies taken from the context and its `@PostConstruct` methods
   * invoked. Each call creates another instance.
   *
   * @return
   *   the instance, whose `close` runs its `@PreDestroy` methods
   * @throws dev.coilix.context.BeanCreationException
   *   if `componentClass` is not collected, is not a `T`, or cannot be created or initialized
   */
  def create[T: TypeInfo](componentClass: Class[?]): ManagedComponent[T]
}
