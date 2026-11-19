package dev.coilix.boot
package modules

import dev.coilix.context.BeanDefinition
import dev.coilix.context.CoilixModule
import dev.coilix.context.DefinitionProcessor

final class RenamingModule extends CoilixModule {
  override def name: String = "renaming"

  override def components: List[Class[?]] = Nil

  override def definitionProcessors: List[DefinitionProcessor] =
    List((definition: BeanDefinition) =>
      if (definition.beanName == "reportTitle") definition.copy(beanName = "monthlyTitle") else definition
    )
}
