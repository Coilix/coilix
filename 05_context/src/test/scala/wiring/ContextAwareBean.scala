package org.scaling.context.wiring

import org.scaling.annotation.Component
import org.scaling.context.ApplicationContext

@Component
final class ContextAwareBean(val context: ApplicationContext)
