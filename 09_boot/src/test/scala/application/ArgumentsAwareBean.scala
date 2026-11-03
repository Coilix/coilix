package dev.coilix.boot.application

import dev.coilix.annotation.Component
import dev.coilix.boot.ApplicationArguments

@Component
final class ArgumentsAwareBean(val arguments: ApplicationArguments)
