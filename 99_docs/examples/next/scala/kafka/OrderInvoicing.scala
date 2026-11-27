package com.example
package kafka

import cats.effect.IO
import dev.coilix.annotation.Service
import dev.coilix.kafka.KafkaClients

@Service
case class OrderInvoicing(clients: KafkaClients) {

  def place(orderId: String, customer: String): IO[Unit] =
    clients.forTopic(ShopTopics.orders).publish(ShopTopics.orders, orderId, customer).void

  def invoiceOrders: IO[Unit] = {
    val kafka = clients.forTopic(ShopTopics.orders)
    kafka
      .subscribe(ShopTopics.orders, group = "invoicing")
      .evalMap(order => invoice(order.record.key, order.record.value).as(order.offset))
      .through(kafka.commits)
      .compile
      .drain
  }

  private def invoice(orderId: String, customer: String): IO[Unit] = IO.unit
}
