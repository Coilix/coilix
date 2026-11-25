package com.example
package processing

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.annotation.Slf4s
import dev.coilix.processing.ProcessorEvent

@Component
@Slf4s
final class FailureAlerts {

  @EventListener(async = true)
  private def onFailure(event: ProcessorEvent.ShardFailed): Unit =
    log.warn(s"Shard ${event.shard} of ${event.processor.name} failed: ${event.failure.message}")
}
