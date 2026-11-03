package dev.coilix.demo

import dev.coilix.annotation.ConfigurationProperties
import scala.concurrent.duration.FiniteDuration

/**
 * Mail settings bound from the `demo.mail` section of `application.yaml`.
 *
 * Shows binding of strings, numbers, lists and durations, and a Scala default argument for a missing key.
 *
 * @param host
 *   mail server host
 * @param port
 *   mail server port
 * @param recipients
 *   addresses that receive mail
 * @param timeout
 *   connection timeout, written as `5s` in the configuration
 * @param useTls
 *   whether to use TLS; not set in the demo configuration, so the default `false` is used
 */
@ConfigurationProperties("demo.mail")
final case class MailSettings(host: String, port: Int, recipients: List[String], timeout: FiniteDuration, useTls: Boolean = false)
