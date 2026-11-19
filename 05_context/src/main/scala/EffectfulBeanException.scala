package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when a context with beans created by effects, such as `IO[A]` or `Resource[IO, A]`, is built by an interpreter that
 * cannot run them.
 *
 * @param definitions
 *   the effectful beans, in creation order
 */
final class EffectfulBeanException(val definitions: List[BeanDefinition])
    extends CoilixException(EffectfulBeanException.message(definitions))

/** The message of [[dev.coilix.context.EffectfulBeanException]]. */
object EffectfulBeanException {

  private def message(definitions: List[BeanDefinition]): String =
    s"Beans ${definitions.map(describe).mkString(", ")} are created by effects, which the synchronous context builder does not " +
      "run. Start the application with an effect module, such as CoilixIOApp from coilix-cats-effect"

  private def describe(definition: BeanDefinition): String =
    definition.result.declaredType.fold(s"'${definition.beanName}'")(effectType => s"'${definition.beanName}' ($effectType)")
}
