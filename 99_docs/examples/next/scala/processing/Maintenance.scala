package com.example
package processing

import dev.coilix.annotation.Component
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.StopResult
import dev.coilix.processing.WorkerState

@Component
final class Maintenance(processors: ProcessorManager) {
  def pauseOrders(): StopResult = processors.stop(ShopProcessor.OrderWatch)

  def resumeOrders(): Unit = processors.start(ShopProcessor.OrderWatch)

  def ordersRunning: Boolean = processors.status(ShopProcessor.OrderWatch).all(WorkerState.Running)
}
