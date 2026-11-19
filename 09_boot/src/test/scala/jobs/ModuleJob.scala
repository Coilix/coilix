package dev.coilix.boot
package jobs

import dev.coilix.boot.collected.Job
import java.beans.JavaBean

@JavaBean
final class ModuleJob extends Job {
  override def name: String = "module"
}
