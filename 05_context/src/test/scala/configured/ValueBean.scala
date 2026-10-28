package org.scaling.context.configured

import org.scaling.annotation.Component
import org.scaling.annotation.Value
import scala.concurrent.duration.FiniteDuration

@Component
final class ValueBean(
  @Value("${server.host}") val host:          String,
  @Value("${server.timeout:5s}") val timeout: FiniteDuration,
  @Value("${server.debug}") val debug:        Option[String]
)
