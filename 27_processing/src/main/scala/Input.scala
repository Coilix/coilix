package dev.coilix.processing

/**
 * One element read from an [[dev.coilix.processing.InputSource]].
 *
 * @param slot
 *   the slot the element belongs to, one of the slots of the shard that reads it
 * @param delivery
 *   how many times the element has been delivered, `1` the first time
 * @param acknowledgement
 *   tells the source that the element is done or must come again
 */
final case class Input[+T](
  value:           T,
  slot:            Int,
  id:              InputId,
  delivery:        Int,
  acknowledgement: Acknowledgement
)
