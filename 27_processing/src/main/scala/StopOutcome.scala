package dev.coilix.processing

/** How the work on a shard ended when it was asked to stop. */
enum StopOutcome {

  /** The work stopped within the timeout. */
  case Stopped

  /** The work did not stop within the timeout and was left behind. */
  case Abandoned
}
