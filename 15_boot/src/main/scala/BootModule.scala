package dev.coilix.boot

import dev.coilix.context.CoilixModule

/**
 * Adds the beans of the boot module, such as `ioRunnerAdapter`, which runs the [[dev.coilix.boot.IOApplicationRunner]] beans
 * with the other runners. The module is part of the core and is on the classpath of `coilix-boot`.
 */
final class BootModule extends CoilixModule {

  override def name: String = "boot"

  override def components: List[Class[?]] = List(classOf[BootConfiguration])
}
