package dev.coilix.demo
package workshop

import scala.concurrent.Future

/** Stores finished job reports. Other beans inject this trait, because the implementation is an `@Async` proxy. */
trait JobArchive {

  /** Stores `report` and returns where it was stored. */
  def archive(report: String): Future[String]
}
