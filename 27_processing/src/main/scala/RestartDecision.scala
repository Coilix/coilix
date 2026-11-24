package dev.coilix.processing

import scala.concurrent.duration.FiniteDuration

/** The decision of a [[dev.coilix.processing.RestartPolicy]] after a failure. */
enum RestartDecision {

  /** Start the worker again after the delay. */
  case Restart(delay: FiniteDuration)

  /** Stop restarting: the shard becomes `Failed`. */
  case GiveUp
}
