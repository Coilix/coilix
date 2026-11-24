package dev.coilix.processing

import scala.concurrent.duration.FiniteDuration

/** What happens when the work on a shard ends by itself, set with `coilix.processors.<name>.on-completion`. */
enum CompletionPolicy {

  /** The shard becomes `Completed`, for work done in one run, such as an export. The default, `on-completion = stop`. */
  case Stop

  /** The work starts again after the delay, set with `on-completion = restart` and `completion-delay`. */
  case Restart(delay: FiniteDuration)
}
