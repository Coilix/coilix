package dev.coilix.context

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
