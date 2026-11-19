package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleId

/** Service jobs the workshop runs. */
object ServiceJob {
  private val engine      = ModuleId("engine")
  private val abs         = ModuleId("abs")
  private val immobilizer = ModuleId("immobilizer")

  /** Inspection after a misfire complaint, with a spare key for the customer. */
  val inspection: List[ServiceStep] =
    List(
      ServiceStep(engine, Action.ReadFaults, "Read engine fault codes"),
      ServiceStep(engine, Action.ReadLiveData, "Read engine live data"),
      ServiceStep(abs, Action.ReadFaults, "Read ABS fault codes"),
      ServiceStep(abs, Action.RunTest, "Test the ABS pump", Map("test" -> "abs-pump")),
      ServiceStep(engine, Action.Adapt, "Adapt the throttle body"),
      ServiceStep(immobilizer, Action.Code, "Program a spare key", Map("key" -> "2"))
    )
}
