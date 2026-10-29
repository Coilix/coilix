package io.github.coilix.context.constructor

import io.github.coilix.context.wiring.Greeter

final class DependencyShapes(
  val required: Greeter,
  val optional: Option[Greeter],
  val many:     List[Greeter],
  val sequence: Seq[Greeter]
)
