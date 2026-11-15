package dev.coilix.common
package validation

/** Violations reported by the collection validators in [[dev.coilix.common.validation.Validator]]. */
object StandardViolations {

  /** Reported when an element of a collection breaks its validator. */
  val invalidElements: Violation =
    Violation("collection.invalid-elements", "Collection contains invalid elements")

  /** Reported when a list that must have elements is empty. */
  val emptyList: Violation =
    Violation("list.empty", "List must not be empty")

  /** Reported when a list has fewer elements than required. */
  val listTooShort: ViolationTemplate[Int] =
    ViolationTemplate[Int]("list.too-short", "List must have at least %s elements")(_.toString)

  /** Reported when a list has more elements than allowed. */
  val listTooLong: ViolationTemplate[Int] =
    ViolationTemplate[Int]("list.too-long", "List must have at most %s elements")(_.toString)
}
