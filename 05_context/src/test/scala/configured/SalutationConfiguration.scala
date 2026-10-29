package io.github.coilix.context.configured

import io.github.coilix.annotation.Bean
import io.github.coilix.annotation.Configuration
import io.github.coilix.annotation.Value

@Configuration
final class SalutationConfiguration {

  @Bean
  def salutation(@Value("${salutation.word:Hello}") word: String): Salutation = WordSalutation(word)

  @Bean("companySignature")
  def signature(salutation: Salutation, settings: ServerSettings): Signature =
    Signature(s"${salutation.salute("team")} from ${settings.host}")
}
