package dev.coilix.testkit
package effects
package ioapp

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

@Component
@Order(2)
final class PrintHeaderRunner extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit = IOAppJournal.record("header printed")
}
