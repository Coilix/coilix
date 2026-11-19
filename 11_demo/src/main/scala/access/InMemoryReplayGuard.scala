package dev.coilix.demo
package access

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import dev.coilix.demo.vehicle.RequestId
import java.util.concurrent.atomic.AtomicReference

/** Keeps claimed requests in memory until the context closes, and logs both ends of its lifecycle. */
@Component
@Slf4s
final class InMemoryReplayGuard extends ReplayGuard {
  private val claimed = AtomicReference(Set.empty[(SessionId, RequestId)])

  override def claim(session: SessionId, request: RequestId): Either[AccessError, Unit] =
    Either.cond(!claimed.getAndUpdate(_ + (session -> request)).contains(session -> request), (), AccessError.ReplayedRequest)

  @PostConstruct
  private def open(): Unit = log.info("Replay guard is watching diagnostic requests")

  @PreDestroy
  private def close(): Unit = log.info(s"Replay guard forgets ${claimed.get.size} claimed requests")
}
