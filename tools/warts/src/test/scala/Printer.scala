package dev.coilix.warts

/** An object whose method has the name of a console method, but writes nowhere. */
object Printer {

  /** Returns `text` unchanged. */
  def print(text: String): String = text
}
