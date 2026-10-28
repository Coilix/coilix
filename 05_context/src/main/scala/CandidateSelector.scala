package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait CandidateSelector {
  def select[A <: BeanDescriptor](requested: Class[?], candidates: List[A]): Option[A]
}

object CandidateSelector {
  given ConfigPath[CandidateSelector] = ConfigPath[CandidateSelector]("scaling.context.candidate-selector")

  def default(using components: ComponentProvider): CandidateSelector =
    components.get[CandidateSelector](PrimaryCandidateSelector())
}
