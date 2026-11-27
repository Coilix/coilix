package dev.coilix.events

import dev.coilix.common.GenericType

/**
 * A predictable reason why a bean cannot receive events, returned when the event bus looks for the listeners of a bean.
 *
 * The event bus is the top layer of this check: it fails the start of the context with the
 * [[dev.coilix.events.EventListenerError.exception]] of the first error.
 */
enum EventListenerError {

  /**
   * Several `@EventListener` methods of a bean have the same name.
   *
   * @param beanName
   *   the bean
   * @param method
   *   the shared method name
   */
  case OverloadedMethod(beanName: String, method: String)

  /**
   * An `@EventListener` method cannot be called with an event, for example because it takes two parameters.
   *
   * @param beanName
   *   the bean
   * @param method
   *   the method name
   * @param problem
   *   what is wrong, such as `must return Unit, not int`
   */
  case InvalidMethod(beanName: String, method: String, problem: String)

  /**
   * An [[dev.coilix.events.ApplicationEventListener]] does not declare the type of its events.
   *
   * @param beanName
   *   the bean
   */
  case UndeclaredEventType(beanName: String)

  /**
   * A listener receives events of a type that is not known at runtime, such as a type variable.
   *
   * @param listener
   *   describes the listener, such as `@EventListener method onOrder of bean 'mailer'`
   * @param eventType
   *   the type of the events
   */
  case UnknownEventType(listener: String, eventType: GenericType)

  /** The description of this error. */
  def message: String =
    this match {
      case OverloadedMethod(beanName, method)       => overloaded(beanName, method)
      case InvalidMethod(beanName, method, problem) => s"${EventListenerError.methodLabel(beanName, method)} $problem"
      case UndeclaredEventType(beanName)            => s"${EventListenerError.traitLabel(beanName)} does not declare the type of its events"
      case UnknownEventType(listener, eventType)    => unknownType(listener, eventType)
    }

  /** The exception that reports this error to code that throws. */
  def exception: InvalidEventListenerException = InvalidEventListenerException(message)

  private def overloaded(beanName: String, method: String): String =
    s"@EventListener methods of bean '$beanName' are overloaded as $method. Give each listener method its own name, " +
      "because the name identifies the listener in logs and in the configuration"

  private def unknownType(listener: String, eventType: GenericType): String =
    s"$listener receives events of type $eventType, which is not known at runtime. Use a concrete event type, for example " +
      "by extending ApplicationEventListener with a type argument in a class instead of a lambda"
}

/** Descriptions of listeners used in the messages of [[dev.coilix.events.EventListenerError]]. */
object EventListenerError {

  /** Describes an `@EventListener` method. */
  def methodLabel(beanName: String, method: String): String = s"@EventListener method $method of bean '$beanName'"

  /** Describes an [[dev.coilix.events.ApplicationEventListener]] bean. */
  def traitLabel(beanName: String): String = s"ApplicationEventListener bean '$beanName'"
}
