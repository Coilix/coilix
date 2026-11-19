package com.example
package replicas

import dev.coilix.annotation.Component
import dev.coilix.cluster.ReplicaIdentity

@Component
final class InvoiceBatches(identity: ReplicaIdentity) {
  def mine(customers: List[Int]): List[Int] =
    customers.filter(customer => customer % identity.replicaCount == identity.self.index)
}
