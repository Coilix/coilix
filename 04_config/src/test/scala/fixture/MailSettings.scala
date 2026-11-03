package dev.coilix.config.fixture

import scala.concurrent.duration.FiniteDuration

final case class MailSettings(
  host:       String,
  port:       Int,
  recipients: List[String],
  timeout:    FiniteDuration,
  useTls:     Boolean = false,
  replyTo:    Option[String]
)
