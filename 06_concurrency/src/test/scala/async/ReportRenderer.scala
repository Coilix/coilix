package dev.coilix.concurrency
package async

import cats.effect.IO

trait ReportRenderer {
  def render(name: String): IO[String]

  def announce(name: String): IO[Unit]

  def fail(): IO[String]

  def failLater(): IO[Unit]

  def archive(name: String): IO[String]

  def hang(): IO[Unit]

  def callerThread(): String
}
