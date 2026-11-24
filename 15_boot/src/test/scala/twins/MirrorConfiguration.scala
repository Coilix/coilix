package dev.coilix.boot
package twins

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class MirrorConfiguration {

  @Bean
  def leftMirror: Mirror = Mirror("left")

  @Bean
  def rightMirror: Mirror = Mirror("right")
}
