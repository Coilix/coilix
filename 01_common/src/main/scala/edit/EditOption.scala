package dev.coilix.common
package edit

/**
 * A change to an optional field in a partial update, such as an HTTP `PATCH` request.
 *
 * `Option[T]` cannot tell "leave the field as it is" apart from "clear the field". `EditOption[T]` has three states:
 *
 *   - [[dev.coilix.common.edit.EditOption.NoEdit]] keeps the current value,
 *   - `SetTo(Some(value))` replaces it with `value`,
 *   - `SetTo(None)` clears it.
 *
 * ```scala
 * import dev.coilix.common.edit.EditOption
 * import dev.coilix.common.edit.EditOption.NoEdit
 *
 * final case class User(name: String, nickname: Option[String])
 *
 * final case class UserChanges(nickname: EditOption[String] = NoEdit)
 *
 * def update(user: User, changes: UserChanges): User =
 *   user.copy(nickname = changes.nickname.applyTo(user.nickname))
 * ```
 *
 * `map`, `flatMap`, `filter` and `collect` transform only `SetTo(Some(value))`. `NoEdit` and `SetTo(None)` pass through
 * unchanged, unless a method says otherwise.
 *
 * @tparam T
 *   the type of the field value
 */
enum EditOption[+T] {

  /** Keeps the current value of the field. */
  case NoEdit

  /** Sets the field to `value`, or clears it when `value` is `None`. */
  case SetTo(value: Option[T] = None)

  /** Applies `f` to the new value. */
  def map[U](f: T => U): EditOption[U] =
    this match {
      case SetTo(Some(value)) => SetTo(Some(f(value)))
      case SetTo(None)        => SetTo(None)
      case NoEdit             => NoEdit
    }

  /** Replaces the edit with the result of `f` applied to the new value. */
  def flatMap[U](f: T => EditOption[U]): EditOption[U] =
    this match {
      case SetTo(Some(value)) => f(value)
      case SetTo(None)        => SetTo(None)
      case NoEdit             => NoEdit
    }

  /** Removes one level of nesting from an edit whose new value is itself an edit. */
  def flatten[U](using nested: T <:< EditOption[U]): EditOption[U] =
    flatMap(nested)

  /**
   * Reduces the edit to one value.
   *
   * @param ifNoEdit
   *   the result for `NoEdit`
   * @param ifCleared
   *   the result for `SetTo(None)`
   * @param f
   *   computes the result from the new value of `SetTo(Some(value))`
   */
  def fold[U](ifNoEdit: => U, ifCleared: => U)(f: T => U): U =
    this match {
      case SetTo(Some(value)) => f(value)
      case SetTo(None)        => ifCleared
      case NoEdit             => ifNoEdit
    }

  /** Applies `pf` to the new value and turns the edit into `NoEdit` when `pf` is not defined for it. */
  def collect[U](pf: PartialFunction[T, U]): EditOption[U] =
    flatMap(value => pf.lift(value).fold[EditOption[U]](NoEdit)(result => SetTo(Some(result))))

  /** Keeps the edit when its new value satisfies `p`, and turns it into `NoEdit` otherwise. */
  def filter(p: T => Boolean): EditOption[T] =
    collect { case value if p(value) => value }

  /** The same as [[filter]], so that an edit can be used in a `for` expression with a guard. */
  def withFilter(p: T => Boolean): EditOption[T] =
    filter(p)

  /**
   * Combines two edits into an edit of a pair.
   *
   * The result is `SetTo(Some((a, b)))` when both edits set a value, `SetTo(None)` when either clears the field, and
   * `NoEdit` otherwise.
   */
  def zip[U](that: EditOption[U]): EditOption[(T, U)] =
    (this, that) match {
      case (SetTo(Some(left)), SetTo(Some(right))) => SetTo(Some((left, right)))
      case (SetTo(None), _) | (_, SetTo(None))     => SetTo(None)
      case _                                       => NoEdit
    }

  /** This edit, or `that` when this edit is `NoEdit`. */
  def orElse[U >: T](that: => EditOption[U]): EditOption[U] =
    this match {
      case NoEdit => that
      case _      => this
    }

  /** The new value, or `default` for `NoEdit` and `SetTo(None)`. */
  def getOrElse[U >: T](default: => U): U =
    toOption.getOrElse(default)

  /** The field value after the edit: `current` for `NoEdit`, and the new value or `None` for `SetTo`. */
  def applyTo[U >: T](current: Option[U]): Option[U] =
    this match {
      case NoEdit       => current
      case SetTo(value) => value
    }

  /** The new value as an `Option`, which is `None` for `NoEdit` and `SetTo(None)`. */
  def toOption: Option[T] =
    this match {
      case SetTo(value) => value
      case NoEdit       => None
    }

  /** All three states as a nested `Option`: `None` for `NoEdit` and `Some(value)` for `SetTo(value)`. */
  def toNestedOption: Option[Option[T]] =
    this match {
      case SetTo(value) => Some(value)
      case NoEdit       => None
    }

  /** The new value as a list with at most one element. */
  def toList: List[T] =
    toOption.toList

  /** Whether this edit is `NoEdit`. */
  def isNoEdit: Boolean =
    this == NoEdit

  /** Whether this edit is `SetTo`, with or without a value. */
  def isSet: Boolean =
    !isNoEdit

  /** Whether this edit sets a value, that is `SetTo(Some(value))`. */
  def isDefined: Boolean =
    toOption.isDefined

  /** Whether this edit does not set a value, that is `NoEdit` or `SetTo(None)`. */
  def isEmpty: Boolean =
    !isDefined

  /** Whether this edit sets a value that satisfies `p`. */
  def exists(p: T => Boolean): Boolean =
    toOption.exists(p)

  /** Whether this edit sets no value or a value that satisfies `p`. */
  def forall(p: T => Boolean): Boolean =
    toOption.forall(p)

  /** Whether this edit sets exactly `value`. */
  def contains[U >: T](value: U): Boolean =
    toOption.contains(value)

  /** Runs `f` on the new value, if there is one. */
  def foreach(f: T => Unit): Unit =
    toOption.foreach(f)
}

/** Factory methods for [[dev.coilix.common.edit.EditOption]]. */
object EditOption {

  /** An edit that sets the field to `value`. */
  def apply[T](value: T): EditOption[T] =
    SetTo(Some(value))

  /** An edit that sets the field to `value`, the same as `apply`. */
  def some[T](value: T): EditOption[T] =
    SetTo(Some(value))

  /** An edit that clears the field. */
  def cleared[T]: EditOption[T] =
    SetTo(None)

  /** An edit that sets the field to `value`, or clears it when `value` is `None`. */
  def fromOption[T](value: Option[T]): EditOption[T] =
    SetTo(value)

  /** An edit from a nested `Option`: `NoEdit` for `None` and `SetTo(value)` for `Some(value)`. */
  def fromNestedOption[T](value: Option[Option[T]]): EditOption[T] =
    value.fold[EditOption[T]](NoEdit)(SetTo(_))
}
