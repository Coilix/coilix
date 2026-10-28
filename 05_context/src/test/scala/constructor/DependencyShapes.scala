package org.scaling.context.constructor

import org.scaling.context.wiring.Greeter

final class DependencyShapes(
  val required: Greeter,
  val optional: Option[Greeter],
  val many:     List[Greeter],
  val sequence: Seq[Greeter]
)
