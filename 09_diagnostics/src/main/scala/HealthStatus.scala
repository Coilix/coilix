package dev.coilix.diagnostics

/** How well the application or one of its parts works, from the best to the worst. */
enum HealthStatus {

  /** Works correctly. */
  case Up

  /** Works, but should not get new work, for example because memory is short. */
  case Degraded

  /** Does not work correctly and will not recover by itself, for example because threads are deadlocked. */
  case Down

  /** The worse of this status and `other`. */
  def worse(other: HealthStatus): HealthStatus = if (other.ordinal > ordinal) other else this
}

/** Combining of [[dev.coilix.diagnostics.HealthStatus]] values. */
object HealthStatus {

  /** The worst of `statuses`, or `Up` when there are none. */
  def worst(statuses: Iterable[HealthStatus]): HealthStatus = statuses.foldLeft(HealthStatus.Up)(_.worse(_))
}
