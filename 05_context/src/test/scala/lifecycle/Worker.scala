package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component

@Component
final class Worker(journal: Journal) extends Startable(journal)
