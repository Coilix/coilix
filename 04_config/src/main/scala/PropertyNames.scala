package io.github.coilix.config

object PropertyNames {

  def candidates(name: String): List[String] =
    List(name, kebabCase(name)).distinct

  def kebabCase(name: String): String =
    name.flatMap(character => if (character.isUpper) s"-${character.toLower}" else character.toString)
}
