package dev.coilix.demo
package vehicle

import dev.coilix.annotation.Component
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.workshop.ModuleSettings

/** Driver for engine control units: fault codes, live data, throttle adaptation and injector coding. */
@Component
final class EngineDriver extends ModuleDriver {

  override def kind: ModuleKind = ModuleKind.Engine

  override def supportedActions: Set[Action] = Set(Action.ReadFaults, Action.ReadLiveData, Action.Adapt, Action.Code)

  override def execute(settings: ModuleSettings, command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.action match {
      case Action.ReadFaults   => Right(ModuleResult.faults(settings.faults))
      case Action.ReadLiveData => Right(ModuleResult(Map("rpm" -> "820", "coolant" -> "91 C", "lambda" -> "1.00")))
      case Action.Adapt        => Right(ModuleResult(Map("throttle-body" -> "adapted")))
      case Action.Code         => code(command)
      case Action.RunTest      => Left(AccessError.ModuleRejected)
    }

  private def code(command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.parameters.get("injectors").map(codes => ModuleResult(Map("injectors" -> codes))).toRight(AccessError.ModuleRejected)
}
