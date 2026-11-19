package dev.coilix.demo
package workshop

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Clock

/** Beans from classes the demo does not own: the signing key of the workshop and the clock. */
@Configuration
final class WorkshopConfiguration {

  /** A new Ed25519 key pair for each start. A real workshop would load it from a hardware security module. */
  @Bean
  def workshopKeys(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

  /** The clock for token times. Tests can replace it with a fixed clock. */
  @Bean
  def clock(): Clock = Clock.systemUTC()
}
