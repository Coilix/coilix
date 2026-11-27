package dev.coilix.demo
package workshop

import cats.effect.IO

/** Stores finished job reports. Other beans inject this trait, because the implementation is an `@Async` proxy. */
trait JobArchive {

  /** Stores `report` and returns where it was stored. */
  def archive(report: String): IO[String]

  /** Adds `report` to the search index in the background; the caller does not wait for it. */
  def index(report: String): IO[Unit]
}
