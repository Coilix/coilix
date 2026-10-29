package io.github.coilix.demo

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Order
import io.github.coilix.annotation.Slf4s
import io.github.coilix.boot.ApplicationArguments
import io.github.coilix.boot.ApplicationRunner

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
