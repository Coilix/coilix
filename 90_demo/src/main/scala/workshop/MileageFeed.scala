package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.processing.InMemoryStream

/**
 * The mileage readings of the fleet, kept in memory with a slot for each VIN, so the readings of one vehicle are handled in
 * order by one shard of [[dev.coilix.demo.workshop.ServiceReminders]].
 */
@Component
final class MileageFeed extends InMemoryStream[MileageReading](256, _.vin.value)
