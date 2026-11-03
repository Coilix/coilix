package dev.coilix.context
package ordering

import dev.coilix.annotation.Service

@Service
final class AlphaService(val audit: Option[Audit])
