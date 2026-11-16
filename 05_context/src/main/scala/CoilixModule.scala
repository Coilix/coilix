package dev.coilix.context

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
 */
trait CoilixModule {

  /** The name used in log messages and in the `coilix.modules.<name>.enabled` key, in kebab case. */
  def name: String

  /** The component classes this module adds to the context. */
  def components: List[Class[?]]
}
