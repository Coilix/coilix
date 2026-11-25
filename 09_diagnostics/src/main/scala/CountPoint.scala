package dev.coilix.diagnostics

import java.time.Instant

/** The number of platform threads at one sample. */
final case class CountPoint(at: Instant, count: Int)
