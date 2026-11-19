package dev.coilix.demo

import dev.coilix.demo.TestWorkshop.engine
import dev.coilix.demo.TestWorkshop.golfVin
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.CertificationAccessPolicy
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.ReadOnlyAccessPolicy
import dev.coilix.demo.vehicle.Action
import munit.FunSuite

final class AccessPoliciesTest extends FunSuite {
  private val golf   = TestWorkshop.vehicle(golfVin)
  private val read   = Set(Grant(engine, Action.ReadFaults))
  private val adapt  = Set(Grant(engine, Action.Adapt))
  private val coding = Set(Grant(engine, Action.Code))

  test("grants by certification") {
    val policy = CertificationAccessPolicy()
    assertEquals(policy.approve(TestWorkshop.apprentice, golf, read), Right(read))
    assertEquals(policy.approve(TestWorkshop.apprentice, golf, adapt), Left(AccessError.NotCertified))
    assertEquals(policy.approve(TestWorkshop.technician, golf, adapt), Right(adapt))
    assertEquals(policy.approve(TestWorkshop.technician, golf, coding), Left(AccessError.NotCertified))
    assertEquals(policy.approve(TestWorkshop.master, golf, coding), Right(coding))
  }

  test("the read-only policy grants only reading, even to a master") {
    val policy = ReadOnlyAccessPolicy()
    assertEquals(policy.approve(TestWorkshop.master, golf, read), Right(read))
    assertEquals(policy.approve(TestWorkshop.master, golf, coding), Left(AccessError.NotCertified))
  }
}
