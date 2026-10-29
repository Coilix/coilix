package io.github.coilix.context.configured

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Value
import scala.concurrent.duration.FiniteDuration

@Component
final class ValueBean(
  @Value("${server.host}") val host:          String,
  @Value("${server.timeout:5s}") val timeout: FiniteDuration,
  @Value("${server.debug}") val debug:        Option[String]
)
