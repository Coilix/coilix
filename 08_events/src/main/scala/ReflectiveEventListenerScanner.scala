package dev.coilix.events

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.EventListener
import dev.coilix.common.GenericType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Finds listeners by reflection: annotated methods of the bean class and its superclasses, including private ones, and the
 * event type of an [[dev.coilix.events.ApplicationEventListener]] read from the generic supertypes of the bean class.
 *
 * A listener that cannot be called with events is returned as an [[dev.coilix.events.EventListenerError]].
 *
 * @param settings
 *   gives the retry policy of each listener
 */
private[events] final class ReflectiveEventListenerScanner(settings: EventsSettings) extends EventListenerScanner {

  private type Found[A] = Either[EventListenerError, A]

  override def listenersOf(beanName: String, bean: AnyRef): Found[List[RegisteredListener]] =
    (annotatedMethods(beanName, bean), implemented(beanName, bean)).mapN((methods, traits) =>
      (methods ++ traits).distinctBy(_.info.name)
    )

  private def annotatedMethods(beanName: String, bean: AnyRef): Found[List[RegisteredListener]] = {
    val methods = hierarchy(bean.getClass)
      .flatMap(_.getDeclaredMethods.toList)
      .filter(method => !method.isBridge && !method.isSynthetic && method.isAnnotationPresent(classOf[EventListener]))
      .distinctBy(method => (method.getName, method.getParameterTypes.toList))
      .sortBy(_.getName)
    uniqueNames(beanName, methods).flatMap(_.traverse(fromMethod(beanName, bean, _)))
  }

  private def hierarchy(beanClass: Class[?]): List[Class[?]] =
    List.unfold(Option[Class[?]](beanClass))(_.map(current => (current, Option(current.getSuperclass))))

  private def uniqueNames(beanName: String, methods: List[Method]): Found[List[Method]] =
    methods
      .groupBy(_.getName)
      .toList
      .sortBy(_._1)
      .collectFirst { case (name, several) if several.sizeIs > 1 => EventListenerError.OverloadedMethod(beanName, name) }
      .toLeft(methods)

  private def fromMethod(beanName: String, bean: AnyRef, method: Method): Found[RegisteredListener] =
    for {
      _         <- problemOf(method).map(EventListenerError.InvalidMethod(beanName, method.getName, _)).toLeft(())
      eventType <-
        known(EventListenerError.methodLabel(beanName, method.getName), GenericType.of(method.getGenericParameterTypes()(0)))
    } yield {
      val annotation = method.getAnnotation(classOf[EventListener])
      RegisteredListener(
        info(beanName, method.getName, eventType, annotation.async),
        event => invoke(bean, method, event)
      )
    }

  private def problemOf(method: Method): Option[String] =
    List(
      Modifier.isStatic(method.getModifiers)                 -> "must not be static",
      (method.getParameterCount != 1)                        -> s"must take exactly one parameter, the event, not ${method.getParameterCount}",
      !classOf[IO[?]].isAssignableFrom(method.getReturnType) -> s"must return IO[Unit], not ${method.getReturnType.getName}",
      !method.trySetAccessible()                             ->
        s"cannot be made accessible, open the package ${method.getDeclaringClass.getPackageName} to Coilix"
    ).collectFirst { case (true, problem) => problem }

  private def implemented(beanName: String, bean: AnyRef): Found[List[RegisteredListener]] =
    bean match {
      case listener: ApplicationEventListener[?] => fromTrait(beanName, listener).map(List(_))
      case _ => Right(Nil)
    }

  private def fromTrait(beanName: String, listener: ApplicationEventListener[?]): Found[RegisteredListener] =
    traitArgument(beanName, listener).map { eventType =>
      val typed = acceptingAll(listener)
      RegisteredListener(info(beanName, "onEvent", eventType, listener.async), event => IO.defer(typed.onEvent(event)))
    }

  /**
   * `listener` typed to accept any event. The cast is unchecked, because the event type is erased, but the bus delivers to
   * the listener only events of the type found by `traitArgument`.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def acceptingAll(listener: ApplicationEventListener[?]): ApplicationEventListener[AnyRef] =
    listener.asInstanceOf[ApplicationEventListener[AnyRef]]

  private def traitArgument(beanName: String, listener: ApplicationEventListener[?]): Found[GenericType] =
    GenericType.of(listener.getClass).asSuper(classOf[ApplicationEventListener[?]]) match {
      case Some(GenericType.Concrete(_, List(eventType))) => known(EventListenerError.traitLabel(beanName), eventType)
      case _                                              => Left(EventListenerError.UndeclaredEventType(beanName))
    }

  private def known(label: String, eventType: GenericType): Found[GenericType] =
    Either.cond(
      eventType != GenericType.Wildcard && eventType.variables.isEmpty,
      eventType,
      EventListenerError.UnknownEventType(label, eventType)
    )

  private def info(beanName: String, method: String, eventType: GenericType, async: Boolean): EventListenerInfo =
    EventListenerInfo(s"$beanName.$method", eventType, async, settings.listener(beanName, method).retry)

  /**
   * The effect returned by the listener method. The cast cannot fail, because the scanner accepts only methods that return
   * `IO`, and the result type is erased.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def invoke(bean: AnyRef, method: Method, event: AnyRef): IO[Unit] =
    IO.defer(method.invoke(bean, event).asInstanceOf[IO[Unit]]).adaptError {
      case invocation: InvocationTargetException => Option(invocation.getCause).getOrElse(invocation)
    }
}
