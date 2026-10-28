package org.scaling.context.configured

import org.scaling.annotation.Bean
import org.scaling.annotation.Configuration
import org.scaling.annotation.Value

@Configuration
final class SalutationConfiguration {

  @Bean
  def salutation(@Value("${salutation.word:Hello}") word: String): Salutation = WordSalutation(word)

  @Bean("companySignature")
  def signature(salutation: Salutation, settings: ServerSettings): Signature =
    Signature(s"${salutation.salute("team")} from ${settings.host}")
}
