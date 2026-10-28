package org.scaling.demo

import org.scaling.annotation.Component
import org.scaling.annotation.Order
import org.scaling.annotation.Slf4s
import org.scaling.boot.ApplicationArguments
import org.scaling.boot.ApplicationRunner

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
