package dev.coilix.boot

import dev.coilix.common.CoilixException

/**
 * Thrown when more than one enabled module gives a [[dev.coilix.context.EffectInterpreter]], because beans made by effects can be
 * created by only one of them.
 *
 * @param modules
 *   the names of the modules that give an interpreter
 */
final class ConflictingEffectInterpretersException(modules: List[String])
    extends CoilixException(
      s"Modules ${modules.mkString(", ")} each create beans made by effects. " +
        "Disable all but one with coilix.modules.<name>.enabled = false"
    )
