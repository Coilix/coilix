package dev.coilix.processing

/** Identifiers of the processors in the tests. */
sealed trait TestProcessor extends ProcessorId

object TestProcessor {
  case object Ticker extends TestProcessor

  case object Exporter extends TestProcessor

  case object Streamer extends TestProcessor

  case object Flaky extends TestProcessor

  case object Twin extends TestProcessor

  case object Orphan extends TestProcessor

  case object Shipping extends TestProcessor

  case object Misfit extends TestProcessor

  case object LegacyName extends TestProcessor {
    override def name: String = "legacy_flow"
  }
}
