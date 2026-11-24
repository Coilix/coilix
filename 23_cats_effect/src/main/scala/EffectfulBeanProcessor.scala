package dev.coilix.catseffect

import dev.coilix.context.BeanDefinition
import dev.coilix.context.DefinitionProcessor

/**
 * Prepares the beans made by an `IO` or a `Resource` for the [[dev.coilix.catseffect.IOEffectInterpreter]].
 *
 * Such a bean depends on `ioRuntime` and `catsEffectSettings`, as with [[dev.coilix.annotation.DependsOn]], so the runtime and
 * the settings exist before the first effect runs. A lazy or prototype bean made by an effect stops the startup with an
 * [[dev.coilix.catseffect.InvalidEffectfulBeanException]].
 */
final class EffectfulBeanProcessor extends DefinitionProcessor {

  private val prerequisites = List("ioRuntime", "catsEffectSettings")

  override def process(definition: BeanDefinition): BeanDefinition =
    if (!definition.result.isEffectful) definition
    else if (!definition.creation.atStartup) throw InvalidEffectfulBeanException(definition.beanName)
    else definition.copy(dependsOn = (definition.dependsOn ++ prerequisites).distinct)
}
