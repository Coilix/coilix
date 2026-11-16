package dev.coilix.demo

import dev.coilix.demo.access.AccessError
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.BrakesDriver
import dev.coilix.demo.vehicle.EngineDriver
import dev.coilix.demo.vehicle.ImmobilizerDriver
import dev.coilix.demo.vehicle.ModuleCommand
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.ModuleKind
import dev.coilix.demo.vehicle.ModuleResult
import dev.coilix.demo.vehicle.RequestId
import dev.coilix.demo.workshop.ModuleSettings
import munit.FunSuite

final class ModuleDriversTest extends FunSuite {
  private val settings = ModuleSettings(ModuleId("engine"), ModuleKind.Engine, List("P0301", "P0420"))

  private def command(action: Action, parameters: (String, String)*): ModuleCommand =
    ModuleCommand(action, RequestId("R-1"), parameters.toMap)

  test("the engine driver reports configured faults and codes injectors") {
    val driver = EngineDriver()
    assertEquals(driver.execute(settings, command(Action.ReadFaults)), Right(ModuleResult(Map("faults" -> "P0301 P0420"))))
    assertEquals(driver.execute(settings, command(Action.Code, "injectors" -> "C1")), Right(ModuleResult(Map("injectors" -> "C1"))))
    assertEquals(driver.execute(settings, command(Action.Code)), Left(AccessError.ModuleRejected))
    assertEquals(driver.execute(settings, command(Action.RunTest)), Left(AccessError.ModuleRejected))
  }

  test("the brakes driver runs only known tests") {
    val driver = BrakesDriver()
    assertEquals(
      driver.execute(settings.copy(faults = Nil), command(Action.ReadFaults)),
      Right(ModuleResult(Map("faults" -> "none")))
    )
    assertEquals(
      driver.execute(settings, command(Action.RunTest, "test" -> "abs-pump")),
      Right(ModuleResult(Map("abs-pump" -> "passed")))
    )
    assertEquals(driver.execute(settings, command(Action.RunTest, "test" -> "horn")), Left(AccessError.ModuleRejected))
    assertEquals(driver.execute(settings, command(Action.Adapt)), Left(AccessError.ModuleRejected))
  }

  test("the immobilizer driver programs keys") {
    val driver = ImmobilizerDriver()
    assertEquals(driver.execute(settings, command(Action.Code, "key" -> "2")), Right(ModuleResult(Map("key" -> "2 learned"))))
    assertEquals(driver.execute(settings, command(Action.ReadLiveData)), Left(AccessError.ModuleRejected))
  }

  test("describes results sorted by key") {
    assertEquals(ModuleResult(Map("rpm" -> "820", "coolant" -> "91 C")).describe, "coolant=91 C, rpm=820")
  }
}
