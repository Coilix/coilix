package io.github.coilix.demo

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Order
import io.github.coilix.annotation.Slf4s
import io.github.coilix.boot.ApplicationArguments
import io.github.coilix.boot.ApplicationRunner
import io.github.coilix.context.ApplicationContext

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
