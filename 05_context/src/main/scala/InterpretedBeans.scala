package dev.coilix.context

/**
 * The beans created by an [[dev.coilix.context.EffectInterpreter]].
 *
 * @param registry
 *   the given registry with the created beans added
 * @param release
 *   closes the created beans in the reverse order of their levels; the context calls it once when it closes, before it destroys
 *   the beans created without the interpreter
 */
final case class InterpretedBeans(registry: BeanRegistry, release: () => Unit)
