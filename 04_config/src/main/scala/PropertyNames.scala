package dev.coilix.config

/** Configuration key names that match a Scala parameter name. */
object PropertyNames {

  /** The keys checked for parameter `name`: the name as written and its kebab-case form, without duplicates. */
  def candidates(name: String): List[String] =
    List(name, kebabCase(name)).distinct

  /** Converts a camel-case name to kebab case, for example `connectTimeout` to `connect-timeout`. */
  def kebabCase(name: String): String =
    name.flatMap(character => if (character.isUpper) s"-${character.toLower}" else character.toString)
}
