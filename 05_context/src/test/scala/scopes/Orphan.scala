package dev.coilix.context
package scopes

import dev.coilix.annotation.Component

@Component
final class Orphan(val missing: Provider[Missing])
