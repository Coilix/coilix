package dev.coilix.testkit
package effects
package warmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class Thermostat(journal: WarmupJournal) {

  @PreDestroy
  def disconnect(): IO[Unit] = IO.raiseError(IllegalStateException("the sensor is gone"))

  @PreDestroy
  def release(): Unit = journal.record("thermostat released")
}
