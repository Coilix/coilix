package dev.coilix.slf4s

import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.Names.TermName

/** The field that the plugin adds to annotated classes and objects. */
object LoggerField {

  /** Name of the field: `log`. */
  val name: TermName = "log".toTermName
}
