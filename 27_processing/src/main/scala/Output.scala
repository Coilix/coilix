package dev.coilix.processing

/** Where a processor sends what it produces, such as a topic, a table or an index. */
trait Output[R] extends AutoCloseable {

  /** Sends `elements`, in order. */
  def emit(elements: List[R]): Unit

  /** Releases the connections of the output. By default it does nothing. */
  override def close(): Unit = ()
}
