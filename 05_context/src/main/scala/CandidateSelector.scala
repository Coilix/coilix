package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Chooses one bean when a dependency or lookup asks for a single bean of a type. */
trait CandidateSelector {

  /**
   * The candidate to use for `requested`, `None` when no candidate is assignable to it, or a
   * [[dev.coilix.context.ContextError.AmbiguousBean]] when several candidates match and none can be chosen.
   */
  def choose[A <: BeanDescriptor](requested: GenericType, candidates: List[A]): Either[ContextError.AmbiguousBean, Option[A]]

  /**
   * The candidate chosen by [[dev.coilix.context.CandidateSelector.choose]], for lookups that throw.
   *
   * @throws dev.coilix.context.AmbiguousBeanException
   *   if several candidates match and none can be chosen
   */
  final def select[A <: BeanDescriptor](requested: GenericType, candidates: List[A]): Option[A] =
    choose(requested, candidates).fold(error => throw error.exception, identity)
}

/** Configuration path and default implementation of [[dev.coilix.context.CandidateSelector]]. */
object CandidateSelector {

  /**
   * Replace the component in the configuration under `coilix.context.candidate-selector`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[CandidateSelector] = ConfigPath[CandidateSelector]("coilix.context.candidate-selector")

  /**
   * The configured implementation, or [[dev.coilix.context.PrimaryCandidateSelector]] when `coilix.context.candidate-selector` is
   * not set.
   */
  def default(using components: ComponentProvider): CandidateSelector =
    components.get[CandidateSelector](PrimaryCandidateSelector())
}
