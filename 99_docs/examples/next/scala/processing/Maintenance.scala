package com.example
package processing

import cats.data.EitherT
import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.processing.ProcessorError
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.StopResult
import dev.coilix.processing.WorkerState

@Component
final class Maintenance(processors: ProcessorManager) {
  def pauseOrders: IO[StopResult] = ProcessorError.raised(processors.stop(ShopProcessor.OrderWatch))

  def resumeOrders: EitherT[IO, ProcessorError, Unit] = processors.start(ShopProcessor.OrderWatch)

  def ordersRunning: IO[Boolean] =
    processors.status(ShopProcessor.OrderWatch).fold(_ => false, _.all(WorkerState.Running))
}
