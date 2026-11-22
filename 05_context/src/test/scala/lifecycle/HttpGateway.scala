package dev.coilix.context
package lifecycle

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy

final class HttpGateway(journal: Journal) extends Gateway {
  override def name: String = "http"

  @PostConstruct
  private def connect(): Unit = journal.record("gateway connected")

  @PreDestroy
  private def disconnect(): Unit = journal.record("gateway disconnected")
}
