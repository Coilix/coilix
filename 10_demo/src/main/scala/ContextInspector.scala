package dev.coilix.demo

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.context.ApplicationContext

@Component
@Order(2)
@Slf4s
final class ContextInspector(context: ApplicationContext) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit = {
    log.info(s"Beans visible via injected context: ${context.beanNames.sorted.mkString(", ")}")
    log.info(s"Primary UserService: ${context.getBean[UserService].getClass.getSimpleName}")
    log.info(s"All UserServices: ${context.getBeansOfType[UserService].map(_.getClass.getSimpleName).mkString(", ")}")
  }
}
