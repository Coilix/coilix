package dev.coilix.boot
package modules

import dev.coilix.context.BeanDefinition
import dev.coilix.context.BeanDestroyer
import dev.coilix.context.BeanInstantiator
import dev.coilix.context.BeanRegistry
import dev.coilix.context.CoilixModule
import dev.coilix.context.EffectInterpreter
import dev.coilix.context.InterpretedBeans

final class EffectModule(override val name: String) extends CoilixModule {
  override def components: List[Class[?]] = Nil

  override def effectInterpreter: Option[EffectInterpreter] = Some(EffectModule.Unused)
}

object EffectModule {
  object Unused extends EffectInterpreter {
    override def interpret(
      levels:       List[List[BeanDefinition]],
      registry:     BeanRegistry,
      instantiator: BeanInstantiator,
      destroyer:    BeanDestroyer
    ): InterpretedBeans = throw IllegalStateException("no bean of the test is made by an effect")
  }
}
