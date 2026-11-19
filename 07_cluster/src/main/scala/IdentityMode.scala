package dev.coilix.cluster

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** How a replica finds out its number, set in `coilix.cluster.identity`. */
@Enumerated(ignoreCase = true)
enum IdentityMode {

  /** The only replica: number `0` of `1`, for local runs and tests. This is the default. */
  @EnumName("single") case Single

  /** The number and the count come from `coilix.cluster.replica-index` and `coilix.cluster.replicas`. */
  @EnumName("static") case Static

  /**
   * The replica is a pod of a Kubernetes StatefulSet. Its number comes from `coilix.cluster.replica-index`, the
   * `COILIX_POD_INDEX` variable filled from the `apps.kubernetes.io/pod-index` label, or the end of the pod name, such as `3`
   * in `shop-3`. The count comes from `coilix.cluster.replicas`.
   */
  @EnumName("stateful-set") case StatefulSet
}
