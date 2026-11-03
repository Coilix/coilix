package dev.coilix.boot

import dev.coilix.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

/**
 * Entry point of a Coilix application.
 *
 * ```scala
 * @CoilixApplication
 * object ShopApplication {
 *   def main(args: Array[String]): Unit = {
 *     val _ = Coilix.run[ShopApplication.type](args)
 *   }
 * }
 * ```
 */
object Coilix {

  /**
   * Starts the application whose class `T` is annotated with `@CoilixApplication`.
   *
   * The steps are: parse `args`, load the configuration, print the banner, scan the packages for components, create the beans,
   * register a shutdown hook that closes the context, and call the [[dev.coilix.boot.ApplicationRunner]] beans.
   *
   * @return
   *   the running context, which stays open until it is closed or the JVM stops
   * @throws dev.coilix.boot.NotABootApplicationException
   *   if `T` is not annotated with `@CoilixApplication`
   * @throws dev.coilix.common.CoilixException
   *   if the configuration or the beans are invalid, for example when a dependency is missing
   */
  def run[T: ClassTag](args: Array[String]): ConfigurableApplicationContext =
    CoilixApplicationRunner.default.run[T](args.toList)
}
