package dev.coilix.demo
package vehicle

import dev.coilix.annotation.Component
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.workshop.ModuleSettings

/** Driver for ABS modules: fault codes, wheel speeds and actuator tests. */
@Component
final class BrakesDriver extends ModuleDriver {
  private val tests = Set("abs-pump", "inlet-valves")

  override def kind: ModuleKind = ModuleKind.Brakes

  override def supportedActions: Set[Action] = Set(Action.ReadFaults, Action.ReadLiveData, Action.RunTest)

  override def execute(settings: ModuleSettings, command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.action match {
      case Action.ReadFaults   => Right(ModuleResult.faults(settings.faults))
      case Action.ReadLiveData => Right(ModuleResult(Map("wheel-speeds" -> "0 0 0 0 km/h")))
      case Action.RunTest      => runTest(command)
      case _                   => Left(AccessError.ModuleRejected)
    }

  private def runTest(command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.parameters.get(
      "test"
    ).filter(tests.contains).map(test => ModuleResult(Map(test -> "passed"))).toRight(AccessError.ModuleRejected)
}
