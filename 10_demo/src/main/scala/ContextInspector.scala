package org.scaling.demo

import org.scaling.annotation.Component
import org.scaling.annotation.Order
import org.scaling.annotation.Slf4s
import org.scaling.boot.ApplicationArguments
import org.scaling.boot.ApplicationRunner
import org.scaling.context.ApplicationContext

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
