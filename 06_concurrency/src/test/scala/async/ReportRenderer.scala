package dev.coilix.concurrency
package async

import scala.concurrent.Future

trait ReportRenderer {
  def render(name: String): Future[String]

  def announce(name: String): Unit

  def fail(): Future[String]

  def archive(name: String): Future[String]

  def callerThread(): String
}
