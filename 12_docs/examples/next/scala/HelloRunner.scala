package com.example

import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

@Component
@Slf4s
final class HelloRunner(@Value("${app.greeting}") greeting: String) extends ApplicationRunner {

  override def run(arguments: ApplicationArguments): Unit =
    log.info(s"$greeting, ${arguments.optionValue("name").getOrElse("World")}")
}
