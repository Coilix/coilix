package dev.coilix.events

/**
 * The stage of a [[dev.coilix.events.DefaultEventBus]] with the events published before the start, kept together so that an
 * event is either kept for the start or delivered, never lost between the two.
 *
 * @param state
 *   the stage of life of the bus
 * @param early
 *   the events published before the start, oldest first
 */
private[events] final case class EventBusStatus(state: EventBusState, early: List[EarlyEvent])
