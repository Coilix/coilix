package dev.coilix.boot
package adapted

import dev.coilix.annotation.Order
import dev.coilix.boot.application.RunJournal

@Order(2)
final class BetweenScript(journal: RunJournal) extends Script(journal, "between")
