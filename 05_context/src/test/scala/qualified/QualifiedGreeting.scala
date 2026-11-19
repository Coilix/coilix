package dev.coilix.context
package qualified

import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier
import dev.coilix.context.wiring.Greeter

@Component
final class QualifiedGreeting(@Qualifier("englishGreeter") val greeter: Greeter)
