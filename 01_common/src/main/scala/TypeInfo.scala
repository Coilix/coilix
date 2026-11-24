package dev.coilix.common

/**
 * The full type of `T`, with its type arguments, available at runtime.
 *
 * A `ClassTag` keeps only the class, so `Repository[IO]` and `Repository[Option]` look the same. A `TypeInfo` is derived by
 * the compiler from the static type, so APIs that look up values by type can tell them apart:
 *
 * ```scala
 * def describe[T: TypeInfo]: String = TypeInfo[T].genericType.toString
 *
 * describe[Repository[IO]]
 * ```
 *
 * For an abstract type `T` the derivation falls back to a `ClassTag[T]` in scope, which gives the class without type
 * arguments. A type lambda or a partially applied type, such as `[A] =>> Either[String, A]`, is a compilation error, because
 * the JVM keeps only `Either` of it.
 *
 * @param genericType
 *   the type of `T`
 */
final class TypeInfo[T](val genericType: GenericType) {

  /**
   * The class of `T`, typed as `Class[T]`.
   *
   * The cast cannot fail, because the erasure of the type of `T` is the class of `T`.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def runtimeClass: Class[T] = genericType.erasure.asInstanceOf[Class[T]]

  /** Whether `value` is an instance of the class of `T`. Type arguments cannot be checked at runtime. */
  def isInstance(value: Any): Boolean = runtimeClass.isInstance(value)
}

/** Derivation and factories of [[dev.coilix.common.TypeInfo]]. */
object TypeInfo {

  /** The type information derived from the static type `T`. */
  inline given derived[T]: TypeInfo[T] = ${ TypeInfoMacros.derive[T] }

  /** The type information in scope for `T`. */
  def apply[T](using info: TypeInfo[T]): TypeInfo[T] = info

  /** The type information for a type built at runtime, for example from reflection. */
  def of[T](genericType: GenericType): TypeInfo[T] = new TypeInfo[T](genericType)

  /** The type information with only the class of `T`, for code that has a `Class` and no static type. */
  def fromClass[T](runtimeClass: Class[?]): TypeInfo[T] = new TypeInfo[T](GenericType.of(runtimeClass))
}
