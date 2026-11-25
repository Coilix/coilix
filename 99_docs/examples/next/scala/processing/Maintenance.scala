package com.example
package processing

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.StopResult
import dev.coilix.processing.WorkerState

@Component
final class Maintenance(processors: ProcessorManager) {
  def pauseOrders: IO[StopResult] = processors.stop(ShopProcessor.OrderWatch)

  def resumeOrders: IO[Unit] = processors.start(ShopProcessor.OrderWatch)

  def ordersRunning: IO[Boolean] = processors.status(ShopProcessor.OrderWatch).map(_.all(WorkerState.Running))
}
