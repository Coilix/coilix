package dev.coilix.demo
package access

import dev.coilix.common.validation.Violation
import dev.coilix.common.validation.ViolationTemplate

/** Reasons an access request is invalid. The codes come from the names of the values. */
object GrantViolations {

  /** The request has no grants. */
  val noGrants: Violation =
    Violation("At least one grant must be requested")

  /** The request names a module the vehicle does not have. */
  val unknownModule: Violation =
    Violation("Every grant must name a module of the vehicle")

  /** The request has more grants than allowed. */
  val tooManyGrants: ViolationTemplate[Int] =
    ViolationTemplate[Int]("At most %s grants can be requested at once")(_.toString)
}
