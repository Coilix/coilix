package dev.coilix.demo

import dev.coilix.annotation.CoilixApplication
import dev.coilix.boot.Coilix

/**
 * Entry point of the demo: a car workshop where mechanics get signed, short-lived tokens to diagnose and code vehicles.
 *
 * Run it with `sbt demo/run`. Add `--mechanic=M-003` to work as an apprentice, `--vin=TMBJJ7NE8J0000002` to inspect the
 * other vehicle, or any `--key=value` option to override the configuration in `application.yaml`.
 *
 * Only the `access`, `vehicle` and `workshop` packages are scanned. The `diagnostics` package is an optional module added
 * through `META-INF/services`, and `--workshop.telematics.enabled=true` turns on the telematics uplink.
 */
@CoilixApplication(scanBasePackages = Array("dev.coilix.demo.access", "dev.coilix.demo.vehicle", "dev.coilix.demo.workshop"))
object DemoApplication {

  /** Starts the demo with the command-line arguments. */
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[DemoApplication.type](args)
  }
}
