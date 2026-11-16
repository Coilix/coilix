package dev.coilix.boot
package modules

import dev.coilix.context.CoilixModule
import dev.coilix.context.ModuleLoader

final class FixedModuleLoader(modules: List[CoilixModule]) extends ModuleLoader {
  override def load(): List[CoilixModule] = modules
}
