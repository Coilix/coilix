package dev.coilix.demo

import dev.coilix.annotation.ConfigurationProperties
import scala.concurrent.duration.FiniteDuration

@ConfigurationProperties("demo.mail")
final case class MailSettings(host: String, port: Int, recipients: List[String], timeout: FiniteDuration, useTls: Boolean = false)
