package dev.coilix.context

/**
 * Chooses a bean in this order:
 *
 *  1. a candidate whose class is exactly the requested type,
 *  1. the only candidate assignable to the requested type,
 *  1. the only assignable candidate marked with `@Primary`.
 *
 * Several assignable candidates without exactly one `@Primary` give [[dev.coilix.context.AmbiguousBeanException]]. This is the
 * default
 * [[dev.coilix.context.CandidateSelector]].
 */
final class PrimaryCandidateSelector extends CandidateSelector {

  override def select[A <: BeanDescriptor](requested: Class[?], candidates: List[A]): Option[A] =
    selectAmong(requested, candidates.filter(candidate => requested.isAssignableFrom(candidate.beanClass)))

  private def selectAmong[A <: BeanDescriptor](requested: Class[?], matching: List[A]): Option[A] =
    matching.find(_.beanClass == requested).orElse(selectAssignable(requested, matching))

  private def selectAssignable[A <: BeanDescriptor](requested: Class[?], matching: List[A]): Option[A] =
    matching match {
      case Nil           => None
      case single :: Nil => Some(single)
      case many          => Some(selectPrimary(requested, many))
    }

  private def selectPrimary[A <: BeanDescriptor](requested: Class[?], many: List[A]): A =
    many.filter(_.isPrimary) match {
      case primary :: Nil => primary
      case _              => throw AmbiguousBeanException(requested, many.map(_.beanName))
    }
}
