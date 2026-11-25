package dev.coilix.testkit
package effects
package fixture

import dev.coilix.annotation.Component
import dev.coilix.concurrency.ContextPropagator
import dev.coilix.concurrency.ContextSnapshot

@Component
final class TenantPropagator extends ContextPropagator {
  override def capture(): ContextSnapshot = {
    val tenant = CurrentTenant.get
    new ContextSnapshot {
      override def runWith[T](task: => T): T = CurrentTenant.withTenant(tenant)(task)
    }
  }
}
