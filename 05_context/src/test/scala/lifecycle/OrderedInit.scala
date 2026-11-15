package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct

@Component
final class OrderedInit(journal: Journal) {

  @PostConstruct
  def second(): Unit = journal.record("second")

  @PostConstruct
  def first(): Unit = journal.record("first")
}
