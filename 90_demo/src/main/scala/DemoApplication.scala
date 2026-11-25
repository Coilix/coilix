package dev.coilix.demo

import dev.coilix.annotation.CoilixApplication
import dev.coilix.boot.CoilixIOApp

/**
 * Entry point of the demo: a car workshop where mechanics get signed, short-lived tokens to diagnose and code vehicles.
 *
 * Run it with `sbt demo/run`. Add `--mechanic=M-003` to work as an apprentice, `--vin=TMBJJ7NE8J0000002` to inspect the
 * other vehicle, or any `--key=value` option to override the configuration in `application.yaml`.
 *
 * Only the `access`, `vehicle` and `workshop` packages are scanned. The `diagnostics` package is an optional module added
 * through `META-INF/services`, and `--workshop.telematics.enabled=true` turns on the telematics uplink.
 *
 * The workshop is a `CoilixIOApp`: its context lives in the main fiber of cats-effect, whose runtime is the `ioRuntime` bean.
 * `coilix.cats-effect.exit-after-runners` in `application.yaml` closes the workshop after the shift, and
 * `--coilix.cats-effect.exit-after-runners=false` keeps it open until it is stopped with Ctrl+C.
 */
@CoilixApplication(scanBasePackages = Array("dev.coilix.demo.access", "dev.coilix.demo.vehicle", "dev.coilix.demo.workshop"))
object DemoApplication extends CoilixIOApp
