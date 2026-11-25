package dev.coilix.boot
package modules

import dev.coilix.context.CoilixModule

/** A module that gives the effect of the application. */
final class EffectTypeModule(override val name: String, effect: Class[?]) extends CoilixModule {
  override def components: List[Class[?]] = Nil

  override def effectType: Option[Class[?]] = Some(effect)
}
