package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.ConditionalOnClass
import dev.coilix.annotation.ConditionalOnProperty
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value

/**
 * Sends job reports to the manufacturer portal, only with `--workshop.telematics.enabled=true`.
 *
 * `@ConditionalOnProperty` leaves the bean out by default, so [[dev.coilix.demo.workshop.WorkshopShift]] gets `None`.
 * `@ConditionalOnClass` also requires the JDK HTTP client. The demo only logs the report instead of sending it.
 */
@Component
@ConditionalOnProperty(name = "workshop.telematics.enabled", havingValue = "true")
@ConditionalOnClass(Array("java.net.http.HttpClient"))
@Slf4s
final class HttpTelematicsUplink(@Value("${workshop.telematics.url}") url: String) extends TelematicsUplink {

  override def send(report: String): Unit = log.info(s"Telematics report to $url: $report")
}
