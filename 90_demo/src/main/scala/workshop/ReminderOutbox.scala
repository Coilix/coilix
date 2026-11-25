package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.processing.InMemoryOutput

/** The service reminders waiting to be sent to the owners. */
@Component
final class ReminderOutbox extends InMemoryOutput[String]
