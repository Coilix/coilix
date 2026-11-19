package dev.coilix.boot
package collected

import java.beans.JavaBean

@JavaBean
final class NightlyJob(log: JobLog) extends Job {
  override def name: String = s"${log.prefix} nightly"
}
