package dev.coilix.boot

import dev.coilix.common.CoilixException

/**
 * Thrown when more than one enabled module gives the effect of the application ([[dev.coilix.context.CoilixModule.effectType]]),
 * because the type parameter of tagless final components can be bound to only one effect.
 *
 * @param modules
 *   the names of the modules that give an effect, each with its effect
 */
final class ConflictingEffectTypesException(modules: List[String])
    extends CoilixException(
      s"Modules ${modules.mkString(", ")} each give the effect of the application. " +
        "Disable all but one with coilix.modules.<name>.enabled = false"
    )
