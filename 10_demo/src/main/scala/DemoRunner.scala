package dev.coilix.demo

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

/**
 * Runner that shows the main kinds of dependencies: a primary bean, an optional bean that does not exist, a list of beans,
 * a bean from a `@Bean` method and a `@ConfigurationProperties` bean.
 *
 * Runs first because of `@Order(1)`.
 */
@Component
@Order(1)
@Slf4s
final class DemoRunner(
  service:      UserService,
  optional:     Option[OptService],
  userServices: List[UserService],
  greeter:      Greeter,
  mail:         MailSettings
) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit = {
    log.info(greeter.greet(arguments.optionValue("name").getOrElse("World")))
    log.info(s"Mail: ${mail.host}:${mail.port}, timeout ${mail.timeout}, recipients ${mail.recipients.mkString(", ")}")
    log.info(s"UserService beans visible: ${userServices.size}")
    log.info(s"OptService present: ${optional.isDefined}")
    service.listUsers().foreach(logUser)
  }

  private def logUser(user: String): Unit =
    log.info(s"User: $user")
}
