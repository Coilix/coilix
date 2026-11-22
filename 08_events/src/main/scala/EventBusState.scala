package dev.coilix.events

/** The stage of life of a [[dev.coilix.events.DefaultEventBus]]. */
private[events] enum EventBusState {

  /** The listeners are not known yet, so published events wait. */
  case Starting

  /** Events are delivered. */
  case Running

  /** Only listeners may publish, and the bus waits for the running asynchronous deliveries. */
  case Closing

  /** No event is accepted. */
  case Closed
}
