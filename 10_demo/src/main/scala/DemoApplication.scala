package dev.coilix.demo

import dev.coilix.annotation.CoilixApplication
import dev.coilix.boot.Coilix

/**
 * Entry point of the demo application.
 *
 * Run it with `sbt demo/run`. Add `--name=Ann` to change the greeting, or any `--key=value` option to override the
 * configuration in `application.yaml`.
 */
@CoilixApplication
object DemoApplication {

  /** Starts the demo with the command-line arguments. */
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[DemoApplication.type](args)
  }
}
