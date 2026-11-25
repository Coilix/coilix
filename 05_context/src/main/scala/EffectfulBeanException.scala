package dev.coilix.context

import dev.coilix.common.CoilixException

/**
 * Thrown when a context with beans created by effects, such as `IO[A]` or `Resource[IO, A]`, or with lifecycle methods that
 * return effects, such as `IO[Unit]`, is built by an interpreter that cannot run them.
 *
 * @param definitions
 *   the effectful beans, in creation order
 */
final class EffectfulBeanException(val definitions: List[BeanDefinition])
    extends CoilixException(EffectfulBeanException.message(definitions))

/** The message of [[dev.coilix.context.EffectfulBeanException]]. */
object EffectfulBeanException {

  private def message(definitions: List[BeanDefinition]): String =
    s"Beans ${definitions.map(describe).mkString(", ")} are created or initialized by effects, which the synchronous context builder does not " +
      "run. Start the application with an effect module, such as CoilixIOApp from coilix-boot"

  private def describe(definition: BeanDefinition): String =
    definition.result.declaredType.fold(withLifecycle(definition))(effectType => s"'${definition.beanName}' ($effectType)")

  private def withLifecycle(definition: BeanDefinition): String = {
    val methods = (definition.lifecycle.effectfulInit ++ definition.lifecycle.effectfulDestroy).map(_.getName).distinct
    s"'${definition.beanName}' (lifecycle methods ${methods.mkString(", ")})"
  }
}
