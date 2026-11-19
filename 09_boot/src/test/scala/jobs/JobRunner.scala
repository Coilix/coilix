package dev.coilix.boot
package jobs

import dev.coilix.annotation.Component
import dev.coilix.boot.collected.Job
import dev.coilix.context.CollectedComponents
import java.beans.JavaBean

@Component
final class JobRunner(components: CollectedComponents) {
  val names: List[String] = components.annotatedWith[JavaBean].map(components.create[Job]).map(_.instance.name)
}
