package dev.coilix.boot
package adapted

import dev.coilix.boot.application.RunJournal

abstract class Script(journal: RunJournal, name: String) {
  def execute(arguments: List[String]): Unit = journal.record(s"$name${arguments.mkString(":", ":", "")}")
}
