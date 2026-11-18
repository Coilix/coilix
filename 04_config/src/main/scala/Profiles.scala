package dev.coilix.config

import scala.util.Try

/**
 * Reads the active profiles from the configuration.
 *
 * Profiles are listed under `coilix.profiles.active`, either as a list or as one text with names separated by commas, so
 * that `--coilix.profiles.active=dev,local` works on the command line.
 */
object Profiles {

  /** The configuration key that lists the active profiles. */
  val ActivePath: String = "coilix.profiles.active"

  /** The active profiles in `tree`, in the order they are listed, without blanks and duplicates. */
  def active(tree: ConfigTree): List[String] =
    if (tree.contains(ActivePath)) listed(tree).map(_.trim).filter(_.nonEmpty).distinct
    else Nil

  private def listed(tree: ConfigTree): List[String] =
    Try(tree.elements(ActivePath).map(_.string(ConfigTree.elementKey))).getOrElse(tree.string(ActivePath).split(',').toList)
}
