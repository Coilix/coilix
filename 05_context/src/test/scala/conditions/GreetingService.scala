package dev.coilix.context
package conditions

import dev.coilix.annotation.Component

@Component
final class GreetingService(val greeter: Greeter)
