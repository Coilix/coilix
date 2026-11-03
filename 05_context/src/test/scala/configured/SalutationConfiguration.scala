package dev.coilix.context
package configured

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Value

@Configuration
final class SalutationConfiguration {

  @Bean
  def salutation(@Value("${salutation.word:Hello}") word: String): Salutation = WordSalutation(word)

  @Bean("companySignature")
  def signature(salutation: Salutation, settings: ServerSettings): Signature =
    Signature(s"${salutation.salute("team")} from ${settings.host}")
}
