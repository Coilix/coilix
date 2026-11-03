package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait CandidateSelector {
  def select[A <: BeanDescriptor](requested: Class[?], candidates: List[A]): Option[A]
}

object CandidateSelector {
  given ConfigPath[CandidateSelector] = ConfigPath[CandidateSelector]("coilix.context.candidate-selector")

  def default(using components: ComponentProvider): CandidateSelector =
    components.get[CandidateSelector](PrimaryCandidateSelector())
}
