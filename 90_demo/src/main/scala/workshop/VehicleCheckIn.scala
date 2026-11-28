package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Slf4s
import dev.coilix.demo.vehicle.Vin
import dev.coilix.processing.Shard
import dev.coilix.processing.StreamProcessor
import dev.coilix.processing.annotation.Processor
import fs2.Pipe
import fs2.Stream
import scala.concurrent.duration.DurationInt

/**
 * Checks in the vehicles of the fleet as a stream and saves them to the [[dev.coilix.demo.workshop.CheckInLog]] in batches of
 * ten, or every hour, and announces each saved batch on Kafka through the [[dev.coilix.demo.workshop.CheckInFeed]].
 *
 * The arrivals never end, so the last batch waits in the flow until the processor stops. Stopping ends the arrivals right
 * after the source, the flow saves the waiting batch and only then the shard is `Stopped`, so no checked-in vehicle is lost.
 * `coilix.processors.vehicle-check-in.drain-timeout` limits how long the batch may take to save.
 */
@Processor
@Slf4s
final class VehicleCheckIn(properties: WorkshopProperties, checkIns: CheckInLog, feed: CheckInFeed)
    extends StreamProcessor[VehicleSettings] {

  override val id: WorkshopProcessor = WorkshopProcessor.VehicleCheckIn

  override def source(shard: Shard): Stream[IO, VehicleSettings] =
    Stream.emits(properties.vehicles) ++ Stream.never[IO]

  override def flow(shard: Shard): Pipe[IO, VehicleSettings, Unit] =
    _.evalTap(vehicle => IO(checkIns.arrive(vehicle.vin))).groupWithin(10, 1.hour).evalMap(batch => saved(batch.toList.map(_.vin)))

  private def saved(batch: List[Vin]): IO[Unit] =
    IO(checkIns.save(batch)) *> feed.announce(batch) *> logF.info(s"Checked in ${batch.map(_.value).mkString(", ")}")
}
