package dev.coilix.context
package collected

import dev.coilix.annotation.ConditionalOnProperty
import java.beans.JavaBean

@JavaBean
@ConditionalOnProperty(name = "tasks.report.enabled", havingValue = "true")
final class ReportTask extends Task {
  override def run(): String = "reporting"
}
