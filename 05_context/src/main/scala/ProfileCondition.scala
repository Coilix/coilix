package dev.coilix.context

import dev.coilix.config.Environment

/**
 * Holds when at least one of the profiles matches: a plain name when that profile is active, a name starting with `!` when it
 * is not. Read from [[dev.coilix.annotation.Profile]].
 *
 * @param profiles
 *   profile names, each optionally preceded by `!`
 */
final case class ProfileCondition(profiles: List[String]) extends BeanCondition {

  override def matches(environment: Environment): Boolean =
    profiles.exists(matchesOne(environment.activeProfiles))

  override def description: String =
    s"one of the profiles ${profiles.mkString(", ")} matches the active profiles"

  private def matchesOne(active: List[String])(profile: String): Boolean =
    if (profile.startsWith("!")) !active.contains(profile.drop(1)) else active.contains(profile)
}
