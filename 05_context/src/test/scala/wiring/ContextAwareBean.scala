package dev.coilix.context
package wiring

import dev.coilix.annotation.Component
import dev.coilix.context.ApplicationContext

@Component
final class ContextAwareBean(val context: ApplicationContext)
