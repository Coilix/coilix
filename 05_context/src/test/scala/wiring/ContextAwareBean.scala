package io.github.coilix.context.wiring

import io.github.coilix.annotation.Component
import io.github.coilix.context.ApplicationContext

@Component
final class ContextAwareBean(val context: ApplicationContext)
