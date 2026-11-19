package dev.coilix.context
package qualified

import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier
import dev.coilix.context.wiring.Greeter

@Component
final class MissingQualifier(@Qualifier("frenchGreeter") val greeter: Greeter)
