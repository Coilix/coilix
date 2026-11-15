package dev.coilix.demo

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Repository
import dev.coilix.annotation.Slf4s

/**
 * A [[dev.coilix.demo.UserRepository]] with three fixed users, in unsorted order.
 *
 * Logs when its users are loaded after creation and when it is closed with the context, to show the bean lifecycle.
 */
@Slf4s
@Repository
final class InMemoryUserRepository extends UserRepository {
  private val users = List("Charlie", "Alice", "Bob")

  override def findAll(): List[String] = users

  @PostConstruct
  private def load(): Unit = log.info(s"Loaded ${users.size} users")

  @PreDestroy
  private def close(): Unit = log.info("Closed the user repository")
}
