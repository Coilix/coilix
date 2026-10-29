package io.github.coilix.boot.application

import io.github.coilix.annotation.Component
import io.github.coilix.boot.ApplicationArguments

@Component
final class ArgumentsAwareBean(val arguments: ApplicationArguments)
