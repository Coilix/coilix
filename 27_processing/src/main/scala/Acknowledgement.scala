package dev.coilix.processing

/**
 * Tells the [[dev.coilix.processing.InputSource]] what became of one element: an acknowledged element is done and is not
 * delivered again, such as a committed offset or a deleted message, while a rejected element is delivered again.
 */
trait Acknowledgement {

  /** Marks the element as done. */
  def acknowledge(): Unit

  /** Gives the element back to the source to be delivered again. By default it does nothing. */
  def reject(): Unit = ()
}
