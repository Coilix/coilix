package dev.coilix.context
package conditions

import dev.coilix.annotation.Component
import dev.coilix.annotation.ConditionalOnClass

@Component
@ConditionalOnClass(Array("java.lang.String"))
final class PresentDriverSupport
