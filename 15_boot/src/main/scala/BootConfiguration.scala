package dev.coilix.boot

import cats.effect.unsafe.IORuntime
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

/** The beans of the boot module: `ioRunnerAdapter`. */
@Configuration
final class BootConfiguration {

  /** Runs the [[dev.coilix.boot.IOApplicationRunner]] beans in one list with the other runners. */
  @Bean
  def ioRunnerAdapter(runtime: IORuntime): IORunnerAdapter = IORunnerAdapter(runtime)
}
