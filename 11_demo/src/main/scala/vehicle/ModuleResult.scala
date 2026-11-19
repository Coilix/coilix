package dev.coilix.demo
package vehicle

/** Values a control module returns for a command. */
final case class ModuleResult(data: Map[String, String]) {

  /** The values as `key=value` pairs, sorted by key. */
  def describe: String =
    data.toList.sorted.map((key, value) => s"$key=$value").mkString(", ")
}

/** Results shared by the module drivers. */
object ModuleResult {

  /** The stored fault codes, or `none`. */
  def faults(codes: List[String]): ModuleResult =
    ModuleResult(Map("faults" -> (if (codes.isEmpty) "none" else codes.mkString(" "))))
}
