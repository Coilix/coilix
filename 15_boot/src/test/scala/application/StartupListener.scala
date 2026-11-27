package dev.coilix.boot
package application

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady

@Component
final class StartupListener(journal: RunJournal) {

  @EventListener
  private def onReady(event: ContextReady): IO[Unit] = IO(journal.record("ready"))

  @EventListener
  private def onStarted(event: ApplicationStarted): IO[Unit] = IO(journal.record("started"))
}
