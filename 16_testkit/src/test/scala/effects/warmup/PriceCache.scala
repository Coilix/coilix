package dev.coilix.testkit
package effects
package warmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy

@Component
final class PriceCache(journal: WarmupJournal) {

  @PostConstruct
  def warm(): IO[Unit] = IO(journal.record("cache warmed"))

  @PostConstruct
  def load(): Unit = journal.record("cache loaded")

  @PreDestroy
  def flush(): IO[Unit] = IO(journal.record("cache flushed"))

  @PreDestroy
  def stop(): Unit = journal.record("cache stopped")
}
