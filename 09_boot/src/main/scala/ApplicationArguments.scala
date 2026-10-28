package org.scaling.boot

final case class ApplicationArguments(
  sourceArgs:    List[String],
  options:       Map[String, List[String]],
  nonOptionArgs: List[String]
) {

  def optionNames: Set[String] = options.keySet

  def containsOption(name: String): Boolean = options.contains(name)

  def optionValues(name: String): List[String] = options.getOrElse(name, Nil)

  def optionValue(name: String): Option[String] = optionValues(name).headOption
}

object ApplicationArguments {
  val empty: ApplicationArguments = ApplicationArguments(Nil, Map.empty, Nil)
}
