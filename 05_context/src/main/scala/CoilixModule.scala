package dev.coilix.context

import java.lang.annotation.Annotation

/**
 * An optional part of Coilix, such as database or web support, that adds its beans when it is on the classpath.
 *
 * A module lists its implementation in `META-INF/services/dev.coilix.context.CoilixModule`, so adding the module to the
 * dependencies of an application is enough to enable it. The application can still disable it with
 * `coilix.modules.<name>.enabled = false`.
 *
 * ```scala
 * final class CacheModule extends CoilixModule {
 *   override def name: String = "cache"
 *
 *   override def components: List[Class[?]] = List(classOf[CacheManager], classOf[CacheConfiguration])
 * }
 * ```
 *
 * The components are read like scanned classes: each needs a component annotation such as `@Component` or
 * `@Configuration`, and `@Bean` methods of configuration classes add their beans too. When a module bean and an
 * application bean have the same type, the usual rules apply, so the application can mark its own bean `@Primary`.
 *
 * A module can also collect classes marked with its own annotation, which then are not beans. The context finds them in the
 * scanned packages and in the components of modules, and gives them to the module through
 * [[dev.coilix.context.CollectedComponents]]:
 *
 * ```scala
 * final class JobsModule extends CoilixModule {
 *   override def name: String = "jobs"
 *
 *   override def components: List[Class[?]] = List(classOf[JobRunner])
 *
 *   override def collects: List[Class[? <: Annotation]] = List(classOf[Job])
 * }
 * ```
 */
trait CoilixModule {

  /** The name used in log messages and in the `coilix.modules.<name>.enabled` key, in kebab case. */
  def name: String

  /** The component classes this module adds to the context. */
  def components: List[Class[?]]

  /**
   * The annotations whose classes this module collects instead of letting them become beans, empty by default.
   *
   * A class with one of these annotations must not have a component annotation too.
   */
  def collects: List[Class[? <: Annotation]] = Nil
}
