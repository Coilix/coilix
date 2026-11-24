package dev.coilix.processing
package misuse

import dev.coilix.annotation.Component

/** A bean that injects a processor, which is not a bean. */
@Component
final class Supervisor(val worker: Worker)
