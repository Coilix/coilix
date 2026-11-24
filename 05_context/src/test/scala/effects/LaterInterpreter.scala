package dev.coilix.context
package effects

import java.util.concurrent.atomic.AtomicReference

final class LaterInterpreter(failing: Boolean = false) extends EffectInterpreter {
  private val entries = AtomicReference(List.empty[String])

  def journal: List[String] = entries.get

  override def interpret(
    levels:       List[List[BeanDefinition]],
    registry:     BeanRegistry,
    instantiator: BeanInstantiator,
    destroyer:    BeanDestroyer
  ): InterpretedBeans = {
    record(s"levels ${levels.map(_.map(_.beanName).mkString("+")).mkString(", ")}")
    if (failing) throw IllegalStateException("effect failed")
    InterpretedBeans(levels.flatten.foldLeft(registry)(created(instantiator)), () => record("released"))
  }

  private def created(instantiator: BeanInstantiator)(registry: BeanRegistry, definition: BeanDefinition): BeanRegistry =
    registry.register(instantiator.complete(definition, valueOf(instantiator.produce(definition, registry))))

  private def valueOf(result: AnyRef): AnyRef =
    result match {
      case Later(value: AnyRef)  => value
      case Scoped(value: AnyRef) => value
      case other                 => other
    }

  private def record(entry: String): Unit = {
    val _ = entries.updateAndGet(_ :+ entry)
  }
}
