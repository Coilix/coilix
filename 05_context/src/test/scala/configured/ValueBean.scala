package dev.coilix.context.configured

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value
import scala.concurrent.duration.FiniteDuration

@Component
final class ValueBean(
  @Value("${server.host}") val host:          String,
  @Value("${server.timeout:5s}") val timeout: FiniteDuration,
  @Value("${server.debug}") val debug:        Option[String]
)
