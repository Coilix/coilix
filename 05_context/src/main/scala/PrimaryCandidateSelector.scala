package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * Chooses a bean in this order:
 *
 *  1. a candidate whose class is exactly the requested type,
 *  1. the only candidate assignable to the requested type,
 *  1. the only assignable candidate marked with `@Primary`.
 *
 * Several assignable candidates without exactly one `@Primary` give a [[dev.coilix.context.ContextError.AmbiguousBean]]. This is the
 * default
 * [[dev.coilix.context.CandidateSelector]].
 */
final class PrimaryCandidateSelector extends CandidateSelector {

  override def choose[A <: BeanDescriptor](
    requested:  GenericType,
    candidates: List[A]
  ): Either[ContextError.AmbiguousBean, Option[A]] =
    selectAmong(requested, candidates.filter(candidate => requested.isAssignableFrom(candidate.beanType)))

  private def selectAmong[A <: BeanDescriptor](
    requested: GenericType,
    matching:  List[A]
  ): Either[ContextError.AmbiguousBean, Option[A]] =
    matching.find(_.beanType == requested).fold(selectAssignable(requested, matching))(exact => Right(Some(exact)))

  private def selectAssignable[A <: BeanDescriptor](
    requested: GenericType,
    matching:  List[A]
  ): Either[ContextError.AmbiguousBean, Option[A]] =
    matching match {
      case Nil           => Right(None)
      case single :: Nil => Right(Some(single))
      case many          => selectPrimary(requested, many).map(Some(_))
    }

  private def selectPrimary[A <: BeanDescriptor](requested: GenericType, many: List[A]): Either[ContextError.AmbiguousBean, A] =
    many.filter(_.isPrimary) match {
      case primary :: Nil => Right(primary)
      case _              => Left(ContextError.AmbiguousBean(requested, many.map(_.beanName)))
    }
}
