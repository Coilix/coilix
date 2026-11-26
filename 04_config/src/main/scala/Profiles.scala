package dev.coilix.config

import cats.syntax.all.*

/**
 * Reads the active profiles from the configuration.
 *
 * Profiles are listed under `coilix.profiles.active`, either as a list or as one text with names separated by commas, so
 * that `--coilix.profiles.active=dev,local` works on the command line.
 */
object Profiles {

  /** The configuration key that lists the active profiles. */
  val ActivePath: String = "coilix.profiles.active"

  /**
   * The active profiles in `tree`, in the order they are listed, without blanks and duplicates.
   *
   * @return
   *   the profiles, or a [[dev.coilix.config.ConfigError]] if the key holds a section or a list of sections
   */
  def active(tree: ConfigTree): Either[ConfigError, List[String]] =
    if (tree.contains(ActivePath)) listed(tree).map(_.map(_.trim).filter(_.nonEmpty).distinct)
    else Right(Nil)

  private def listed(tree: ConfigTree): Either[ConfigError, List[String]] =
    tree
      .elements(ActivePath)
      .flatMap(_.traverse(_.string(ConfigTree.elementKey)))
      .orElse(tree.string(ActivePath).map(_.split(',').toList))
}
