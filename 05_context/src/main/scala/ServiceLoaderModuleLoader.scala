package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import java.util.ServiceLoader
import scala.jdk.CollectionConverters.*

/**
 * Finds modules listed in `META-INF/services/dev.coilix.context.CoilixModule` files through `java.util.ServiceLoader`.
 *
 * A module is skipped when `coilix.modules.<name>.enabled` is `false`. Each loaded and each skipped module is logged at
 * `INFO` level. This is the default [[dev.coilix.context.ModuleLoader]], selected when `coilix.context.module-loader` is not
 * set.
 *
 * @param classLoader
 *   the class loader whose resources list the modules
 * @param environment
 *   the configuration that can disable modules
 */
@Slf4s
final class ServiceLoaderModuleLoader(classLoader: ClassLoader, environment: Environment) extends ModuleLoader {

  override def load(): List[CoilixModule] =
    ServiceLoader.load(classOf[CoilixModule], classLoader).asScala.toList.sortBy(_.name).filter(enabled)

  private def enabled(module: CoilixModule): Boolean = {
    val key       = s"coilix.modules.${module.name}.enabled"
    val isEnabled = environment.get[Boolean](key).getOrElse(true)
    if (isEnabled) log.info(s"Loaded module ${module.name}") else log.info(s"Skipped module ${module.name}, disabled by $key")
    isEnabled
  }
}
