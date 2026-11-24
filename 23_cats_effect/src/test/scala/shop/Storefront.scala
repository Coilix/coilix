package dev.coilix.catseffect
package shop

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class Storefront(val catalog: Catalog, val greeting: Greeting, journal: OpeningJournal) {
  journal.record("storefront created")

  @PreDestroy
  def close(): Unit = journal.record("storefront closed")
}
