package dev.coilix.events

import dev.coilix.annotation.EventListener
import dev.coilix.common.GenericType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Finds listeners by reflection: annotated methods of the bean class and its superclasses, including private ones, and the
 * event type of an [[dev.coilix.events.ApplicationEventListener]] read from the generic supertypes of the bean class.
 *
 * @param settings
 *   gives the retry policy of each listener
 */
private[events] final class ReflectiveEventListenerScanner(settings: EventsSettings) extends EventListenerScanner {

  override def listenersOf(beanName: String, bean: AnyRef): List[RegisteredListener] =
    (annotatedMethods(beanName, bean) ++ implemented(beanName, bean)).distinctBy(_.info.name)

  private def annotatedMethods(beanName: String, bean: AnyRef): List[RegisteredListener] = {
    val methods = hierarchy(bean.getClass)
      .flatMap(_.getDeclaredMethods.toList)
      .filter(method => !method.isBridge && !method.isSynthetic && method.isAnnotationPresent(classOf[EventListener]))
      .distinctBy(method => (method.getName, method.getParameterTypes.toList))
      .sortBy(_.getName)
    checkUniqueNames(beanName, methods)
    methods.map(method => fromMethod(beanName, bean, method))
  }

  private def hierarchy(beanClass: Class[?]): List[Class[?]] =
    List.unfold(Option[Class[?]](beanClass))(_.map(current => (current, Option(current.getSuperclass))))

  private def checkUniqueNames(beanName: String, methods: List[Method]): Unit =
    methods.groupBy(_.getName).collectFirst { case (name, several) if several.size > 1 => name }.foreach { name =>
      throw InvalidEventListenerException(
        s"@EventListener methods of bean '$beanName' are overloaded as $name. Give each listener method its own name, " +
          "because the name identifies the listener in logs and in the configuration"
      )
    }

  private def fromMethod(beanName: String, bean: AnyRef, method: Method): RegisteredListener = {
    val label      = s"@EventListener method ${method.getName} of bean '$beanName'"
    problemOf(method).foreach(problem => throw InvalidEventListenerException(s"$label $problem"))
    val eventType  = GenericType.of(method.getGenericParameterTypes()(0))
    checkEventType(label, eventType)
    val annotation = method.getAnnotation(classOf[EventListener])
    RegisteredListener(
      info(beanName, method.getName, eventType, annotation.async, annotation.blocking),
      event                           => invoke(bean, method, event)
    )
  }

  private def problemOf(method: Method): Option[String] =
    List(
      Modifier.isStatic(method.getModifiers) -> "must not be static",
      (method.getParameterCount != 1)        -> s"must take exactly one parameter, the event, not ${method.getParameterCount}",
      (method.getReturnType != Void.TYPE)    -> s"must return Unit, not ${method.getReturnType.getName}",
      !method.trySetAccessible()             ->
        s"cannot be made accessible, open the package ${method.getDeclaringClass.getPackageName} to Coilix"
    ).collectFirst { case (true, problem) => problem }

  private def implemented(beanName: String, bean: AnyRef): List[RegisteredListener] =
    bean match {
      case listener: ApplicationEventListener[?] => List(fromTrait(beanName, listener))
      case _ => Nil
    }

  private def fromTrait(beanName: String, listener: ApplicationEventListener[?]): RegisteredListener = {
    val label     = s"ApplicationEventListener bean '$beanName'"
    val eventType = traitArgument(label, listener)
    val typed     = listener.asInstanceOf[ApplicationEventListener[AnyRef]]
    RegisteredListener(info(beanName, "onEvent", eventType, listener.async, listener.blocking), event => typed.onEvent(event))
  }

  private def traitArgument(label: String, listener: ApplicationEventListener[?]): GenericType =
    GenericType.of(listener.getClass).asSuper(classOf[ApplicationEventListener[?]]) match {
      case Some(GenericType.Concrete(_, List(eventType))) =>
        checkEventType(label, eventType)
        eventType
      case _                                              => throw InvalidEventListenerException(s"$label does not declare the type of its events")
    }

  private def checkEventType(label: String, eventType: GenericType): Unit =
    if (eventType == GenericType.Wildcard || eventType.variables.nonEmpty) {
      throw InvalidEventListenerException(
        s"$label receives events of type $eventType, which is not known at runtime. Use a concrete event type, for example " +
          "by extending ApplicationEventListener with a type argument in a class instead of a lambda"
      )
    }

  private def info(beanName: String, method: String, eventType: GenericType, async: Boolean, blocking: Boolean): EventListenerInfo =
    EventListenerInfo(s"$beanName.$method", eventType, async, settings.listener(beanName, method).retry, blocking)

  private def invoke(bean: AnyRef, method: Method, event: AnyRef): Unit =
    try {
      val _ = method.invoke(bean, event)
    } catch {
      case invocation: InvocationTargetException => throw Option(invocation.getCause).getOrElse(invocation)
    }
}
