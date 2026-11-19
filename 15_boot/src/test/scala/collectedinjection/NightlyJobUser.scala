package dev.coilix.boot
package collectedinjection

import dev.coilix.annotation.Component
import dev.coilix.boot.collected.NightlyJob

@Component
final class NightlyJobUser(job: NightlyJob) {
  def jobName: String = job.name
}
