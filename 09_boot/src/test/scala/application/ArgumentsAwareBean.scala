package org.scaling.boot.application

import org.scaling.annotation.Component
import org.scaling.boot.ApplicationArguments

@Component
final class ArgumentsAwareBean(val arguments: ApplicationArguments)
