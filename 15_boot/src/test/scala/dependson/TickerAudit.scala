package dev.coilix.boot
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.DependsOn

@Component
@DependsOn(Array("ticker"))
final class TickerAudit
