package com.example
package logging

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.logging.LogLevel
import dev.coilix.logging.LogLevels

@Component
final class Troubleshooting(levels: LogLevels) {
  def traceBilling: IO[Unit] = levels.set("com.example.billing", LogLevel.Debug)

  def silenceKafka: IO[Unit] = levels.set("org.apache.kafka", LogLevel.Off)

  def backToConfiguration: IO[Unit] = levels.reset("com.example.billing") *> levels.reset("org.apache.kafka")
}
