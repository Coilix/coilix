package dev.coilix.cluster

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import munit.FunSuite

final class ReplicaIdentityResolverTest extends FunSuite {
  private val now = Instant.parse("2026-11-19T12:00:00Z")

  private def resolve(settings: ClusterSettings, variables: (String, String)*): ReplicaIdentity =
    ReplicaIdentityResolver(variables.toMap, Clock.fixed(now, ZoneOffset.UTC)).resolve(settings)

  private def failure(settings: ClusterSettings, variables: (String, String)*): String =
    intercept[ReplicaIdentityException](resolve(settings, variables*)).getMessage

  test("a single replica is number 0 of 1") {
    assertEquals(
      resolve(ClusterSettings(version = "1.2.0")),
      FixedReplicaIdentity(Member(0, "replica-0", None, now, "1.2.0"), 1)
    )
  }

  test("the replica name comes from the setting, then from HOSTNAME") {
    assertEquals(
      (
        resolve(ClusterSettings(replicaId = Some("api-a")), "HOSTNAME" -> "host").self.id,
        resolve(ClusterSettings(), "HOSTNAME" -> "host").self.id
      ),
      ("api-a", "host")
    )
  }

  test("static mode reads the number and the count from the settings") {
    val identity = resolve(ClusterSettings(IdentityMode.Static, replicas = Some(3), replicaIndex = Some(2)))
    assertEquals((identity.self.index, identity.replicaCount), (2, 3))
  }

  test("static mode falls back to environment variables") {
    val identity = resolve(ClusterSettings(IdentityMode.Static), "COILIX_REPLICAS" -> "4", "COILIX_REPLICA_INDEX" -> " 1 ")
    assertEquals((identity.self.index, identity.replicaCount), (1, 4))
  }

  test("static mode without an index stops with a hint") {
    assert(failure(ClusterSettings(IdentityMode.Static, replicas = Some(2))).contains("coilix.cluster.replica-index"))
  }

  test("a variable that is not a number stops with its name") {
    assert(failure(
      ClusterSettings(IdentityMode.Static, replicaIndex = Some(0)),
      "COILIX_REPLICAS" -> "three"
    ).contains("COILIX_REPLICAS"))
  }

  test("stateful-set mode reads the number from the pod-index variable") {
    val identity =
      resolve(ClusterSettings(IdentityMode.StatefulSet, replicas = Some(3)), "COILIX_POD_INDEX" -> "1", "HOSTNAME" -> "shop-2")
    assertEquals((identity.self.index, identity.self.id), (1, "shop-2"))
  }

  test("stateful-set mode reads the number from the end of the pod name") {
    val identity = resolve(ClusterSettings(IdentityMode.StatefulSet, replicas = Some(3)), "HOSTNAME" -> "shop-api-2")
    assertEquals(identity.self.index, 2)
  }

  test("stateful-set mode without a pod index or a numbered name stops with a hint") {
    val message = failure(ClusterSettings(IdentityMode.StatefulSet, replicas = Some(3)), "HOSTNAME" -> "shop")
    assert(message.contains("COILIX_POD_INDEX"), message)
  }

  test("stateful-set mode needs the number of replicas") {
    assert(failure(ClusterSettings(IdentityMode.StatefulSet), "HOSTNAME" -> "shop-0").contains("coilix.cluster.replicas"))
  }

  test("an index outside the replicas stops the startup") {
    val message = failure(ClusterSettings(IdentityMode.StatefulSet, replicas = Some(3)), "HOSTNAME" -> "shop-3")
    assert(message.contains("Replica index 3 is outside 0 until 3"), message)
  }

  test("a count below one stops the startup") {
    assert(failure(ClusterSettings(IdentityMode.Static, replicas = Some(0), replicaIndex = Some(0))).contains("at least 1"))
  }
}
