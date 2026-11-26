package dev.coilix.logging

/** Short forms of logger names for the log, such as `d.c.d.w.FaultCodeExport` for `dev.coilix.demo.workshop.FaultCodeExport`. */
object LoggerNames {

  /** The name with every package shortened to its first letter and without the `$` of Scala objects. */
  def shortened(name: String): String =
    name.split('.').toList.reverse match {
      case last :: packages if packages.nonEmpty =>
        (packages.reverse.map(_.take(1).toLowerCase) :+ stripped(last)).mkString(".")
      case _                                     => stripped(name)
    }

  private def stripped(name: String): String = name.replace("$", "")
}
