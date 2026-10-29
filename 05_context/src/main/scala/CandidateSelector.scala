package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait CandidateSelector {
  def select[A <: BeanDescriptor](requested: Class[?], candidates: List[A]): Option[A]
}

object CandidateSelector {
  given ConfigPath[CandidateSelector] = ConfigPath[CandidateSelector]("coilix.context.candidate-selector")

  def default(using components: ComponentProvider): CandidateSelector =
    components.get[CandidateSelector](PrimaryCandidateSelector())
}
