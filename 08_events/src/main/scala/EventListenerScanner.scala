package dev.coilix.events

/** Finds the listeners of one bean. */
private[events] trait EventListenerScanner {

  /**
   * The listeners of `bean`: its methods annotated with [[dev.coilix.annotation.EventListener]] and the bean itself when it is
   * an [[dev.coilix.events.ApplicationEventListener]].
   *
   * @throws dev.coilix.events.InvalidEventListenerException
   *   if a listener cannot be called with events
   */
  def listenersOf(beanName: String, bean: AnyRef): List[RegisteredListener]
}
