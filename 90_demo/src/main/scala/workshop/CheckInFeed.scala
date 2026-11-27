package dev.coilix.demo
package workshop

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ContextLifecycleListener
import dev.coilix.demo.vehicle.Vin
import dev.coilix.kafka.KafkaClient
import dev.coilix.kafka.KafkaClients

/**
 * Announces the checked-in vehicles on the Kafka topic [[dev.coilix.demo.workshop.WorkshopTopics.checkIns]], so the owner app
 * and the parts desk learn about them.
 *
 * The topic lives on the client named in the topic, `default`. When `coilix.kafka.clients` does not configure it, as in the
 * demo started without Kafka, the feed announces nothing. When the context starts, the admin client of the cluster creates the
 * topic unless it exists.
 *
 * @param clients
 *   the Kafka clients of the application
 */
@Component
@Slf4s
case class CheckInFeed(clients: KafkaClients) extends ContextLifecycleListener {

  /** Publishes one record per vehicle, keyed by its VIN, and waits until Kafka confirms them. */
  def announce(vehicles: List[Vin]): IO[Unit] =
    kafka.traverse_(client => client.publishAll(WorkshopTopics.checkIns, vehicles.map(_.value -> "reception")))

  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    kafka.fold(logF.info("No Kafka client for check-ins; the feed announces nothing"))(createTopic)

  private def kafka: Option[KafkaClient] = clients.find(WorkshopTopics.checkIns.client)

  private def createTopic(client: KafkaClient): IO[Unit] =
    client.admin.createTopics(List(WorkshopTopics.checkIns))
}
