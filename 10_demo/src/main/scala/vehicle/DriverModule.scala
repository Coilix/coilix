package dev.coilix.demo
package vehicle

import dev.coilix.demo.access.AccessError
import dev.coilix.demo.workshop.ModuleSettings

/**
 * A configured module that delegates to the driver for its kind.
 *
 * @param settings
 *   the module from the configuration
 * @param driver
 *   the driver for `settings.kind`
 */
final class DriverModule(settings: ModuleSettings, driver: ModuleDriver) extends Module {

  override def id: ModuleId = settings.id

  override def supportedActions: Set[Action] = driver.supportedActions

  override def execute(command: ModuleCommand): Either[AccessError, ModuleResult] =
    driver.execute(settings, command)
}
