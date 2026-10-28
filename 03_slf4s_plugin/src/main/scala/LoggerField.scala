package org.scaling.slf4s

import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.Names.TermName

object LoggerField {
  val name: TermName = "log".toTermName
}
