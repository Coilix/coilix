package dev.coilix.demo

import dev.coilix.demo.access.MechanicId
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.Vin
import munit.FunSuite

final class ValueTypesTest extends FunSuite {

  test("accepts a valid VIN") {
    assertEquals(Vin("WVWZZZ1KZ6W000001").value, "WVWZZZ1KZ6W000001")
  }

  test("rejects a VIN with the wrong length or with I, O or Q") {
    List("WVWZZZ1KZ6W00001", "WVWZZZ1KZ6W00000I", "wvwzzz1kz6w000001").foreach { vin =>
      intercept[IllegalArgumentException](Vin(vin))
    }
  }

  test("checks mechanic and module identifiers") {
    assertEquals(MechanicId("M-001").value, "M-001")
    assertEquals(ModuleId("abs").value, "abs")
    intercept[IllegalArgumentException](MechanicId("001"))
    intercept[IllegalArgumentException](ModuleId("ABS"))
  }
}
