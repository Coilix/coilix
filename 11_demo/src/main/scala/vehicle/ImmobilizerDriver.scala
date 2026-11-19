package dev.coilix.demo
package vehicle

import dev.coilix.annotation.Component
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.workshop.ModuleSettings

/** Driver for immobilizers: fault codes and key programming. */
@Component
final class ImmobilizerDriver extends ModuleDriver {

  override def kind: ModuleKind = ModuleKind.Immobilizer

  override def supportedActions: Set[Action] = Set(Action.ReadFaults, Action.Code)

  override def execute(settings: ModuleSettings, command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.action match {
      case Action.ReadFaults => Right(ModuleResult.faults(settings.faults))
      case Action.Code       => programKey(command)
      case _                 => Left(AccessError.ModuleRejected)
    }

  private def programKey(command: ModuleCommand): Either[AccessError, ModuleResult] =
    command.parameters.get("key").map(key => ModuleResult(Map("key" -> s"$key learned"))).toRight(AccessError.ModuleRejected)
}
