package dev.coilix.diagnostics

import java.time.Instant

/** The state of a thread and the time of the first sample that saw the thread in it. */
final case class StateSince(state: Thread.State, since: Instant)
