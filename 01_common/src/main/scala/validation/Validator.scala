package dev.coilix.common
package validation

import scala.reflect.TypeTest

/**
 * A list of rules checked together, reporting every broken rule instead of stopping at the first one.
 *
 * ```scala
 * import dev.coilix.common.validation.Validator
 * import dev.coilix.common.validation.Violation
 *
 * final case class User(name: String, age: Int)
 *
 * val blankName   = Violation("Name must not be blank")
 * val negativeAge = Violation("Age must not be negative")
 *
 * val users: Validator[User] = Validator[User]
 *   .test(_.name.nonEmpty)(blankName)
 *   .testNot(_.age < 0)(negativeAge)
 *
 * users.validate(User("", -1))
 * ```
 *
 * Rules are checked in the order they were added, and violations are reported in the same order.
 */
final case class Validator[T] private (statements: List[Statement[T]]) {

  /** The same as [[validate]]. */
  def apply(value: T): Either[List[Violation], T] =
    validate(value)

  /** `Right(value)` when every rule holds, or `Left` with the violations of the broken rules. */
  def validate(value: T): Either[List[Violation], T] =
    violations(value) match {
      case Nil    => Right(value)
      case broken => Left(broken)
    }

  /** The violations of the rules that `value` breaks, empty when it is valid. */
  def violations(value: T): List[Violation] =
    statements.filterNot(_.holds(value)).map(_.violation)

  /** Whether `value` follows every rule. */
  def isValid(value: T): Boolean =
    statements.forall(_.holds(value))

  /**
   * Returns `value` when it follows every rule.
   *
   * @throws dev.coilix.common.validation.ValidationException
   *   with every violation, if `value` breaks a rule
   */
  def validOrThrow(value: T): T =
    validate(value).fold(broken => throw ValidationException(broken), identity)

  /** Adds a rule that reports `violation` when `condition` is `false`. */
  def test(condition: T => Boolean)(violation: Violation): Validator[T] =
    copy(statements = statements :+ Statement(condition, violation))

  /** Adds a rule that reports `violation` when `condition` is `true`. */
  def testNot(condition: T => Boolean)(violation: Violation): Validator[T] =
    test(value => !condition(value))(violation)

  /** Adds a rule that reports `violation` unless every condition is `true`. */
  def testAll(conditions: (T => Boolean)*)(violation: Violation): Validator[T] =
    test(value => conditions.forall(_(value)))(violation)

  /** Adds a rule that reports `violation` unless at least one condition is `true`. */
  def testAny(conditions: (T => Boolean)*)(violation: Violation): Validator[T] =
    test(value => conditions.exists(_(value)))(violation)

  /** The same as [[combine]] with one validator. */
  def and(other: Validator[T]): Validator[T] =
    combine(other)

  /** Adds the rules of `others` after the rules of this validator. */
  def combine(others: Validator[T]*): Validator[T] =
    copy(statements = statements ++ others.flatMap(_.statements))

  /** Adds the rules of `validator`, checked on the part of the value that `part` selects. */
  def subValidator[S](part: T => S, validator: Validator[S]): Validator[T] =
    copy(statements = statements ++ validator.statements.map(_.contramap(part)))

  /**
   * Adds the rules of `validator` for the field `field`, so that their violations get `field` in front of their path.
   *
   * ```scala
   * val users = Validator[User].field("address", _.address, addresses)
   * ```
   */
  def field[S](field: String, part: T => S, validator: Validator[S]): Validator[T] =
    subValidator(part, validator.under(field))

  /** Adds the rules of `validator`, checked only when the value is an `S`. Other values pass them. */
  def onCaseOf[S <: T](validator: Validator[S])(using TypeTest[T, S]): Validator[T] =
    copy(statements = statements ++ validator.statements.map(onlyFor[S]))

  /** A validator of optional values that checks these rules on a present value and accepts `None`. */
  def optional: Validator[Option[T]] =
    new Validator(statements.map(statement => Statement[Option[T]](_.forall(statement.holds), statement.violation)))

  /** The same rules, with `prefix` added in front of the path of every violation. */
  def under(prefix: String): Validator[T] =
    new Validator(statements.map(statement => statement.copy(violation = statement.violation.under(prefix))))

  private def onlyFor[S <: T](statement: Statement[S])(using TypeTest[T, S]): Statement[T] =
    Statement(
      {
        case value: S => statement.holds(value)
        case _ => true
      },
      statement.violation
    )
}

/** Creates validators, including ready ones for lists. */
object Validator {

  /** A validator without rules, which accepts every value. */
  def apply[T]: Validator[T] =
    new Validator[T](Nil)

  /** A validator with the given rules. */
  def of[T](statements: Statement[T]*): Validator[T] =
    new Validator[T](statements.toList)

  /** A validator of lists that reports one violation when any element breaks `validator`. */
  def validateEach[T](validator: Validator[T]): Validator[List[T]] =
    Validator[List[T]].test(_.forall(validator.isValid))(StandardViolations.collectionInvalidElements)

  /** A validator of lists that rejects an empty list. */
  def validateNonEmpty[T]: Validator[List[T]] =
    Validator[List[T]].test(_.nonEmpty)(StandardViolations.listEmpty)

  /** A validator of lists that rejects a list with fewer than `minSize` elements. */
  def validateMinSize[T](minSize: Int): Validator[List[T]] =
    Validator[List[T]].test(_.sizeIs >= minSize)(StandardViolations.listTooShort(minSize))

  /** A validator of lists that rejects a list with more than `maxSize` elements. */
  def validateMaxSize[T](maxSize: Int): Validator[List[T]] =
    Validator[List[T]].test(_.sizeIs <= maxSize)(StandardViolations.listTooLong(maxSize))
}
