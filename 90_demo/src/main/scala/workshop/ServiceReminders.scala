package dev.coilix.demo
package workshop

import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.processing.ElementContext
import dev.coilix.processing.InputSource
import dev.coilix.processing.Shard
import dev.coilix.processing.StatelessProcessor
import dev.coilix.processing.annotation.Processor
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Reads the mileage of the fleet on two shards and puts a reminder into the outbox when a vehicle passes the service interval,
 * `workshop.service-interval-km` (15 000 km by default). Each reading is acknowledged after it is handled.
 */
@Processor
@Slf4s
final class ServiceReminders(
  feed:                                                     MileageFeed,
  outbox:                                                   ReminderOutbox,
  @Value("${workshop.service-interval-km:15000}") interval: Int
) extends StatelessProcessor[MileageReading] {

  override val id: WorkshopProcessor = WorkshopProcessor.ServiceReminders

  override def defaultShards: Int = 2

  override def pollTimeout: FiniteDuration = 200.millis

  override def source(shard: Shard): InputSource[MileageReading] = feed.source(shard)

  override def process(reading: MileageReading, context: ElementContext): Unit =
    if (reading.kilometres >= interval) {
      log.info(s"${reading.vin.value} passed $interval km, shard ${context.shard} sends a reminder (${context.id})")
      outbox.emit(List(s"${reading.vin.value}: service due at ${reading.kilometres} km"))
    }
}
