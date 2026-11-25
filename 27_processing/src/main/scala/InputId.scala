package dev.coilix.processing

/**
 * A stable identifier of an element of an [[dev.coilix.processing.InputSource]], the same each time the element is delivered,
 * such as `orders-3-1842` for a topic, a partition and an offset, or a file path with its modification time.
 */
final case class InputId(value: String) {

  override def toString: String = value
}
