package dev.coilix.config
package valuetype

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Type
import scala.util.Try

/**
 * How Coilix creates a [[dev.coilix.annotation.ValueType]] class from its inner value and reads the inner value back.
 *
 * Instances come from a [[dev.coilix.config.valuetype.ValueTypeResolver]].
 *
 * @param valueType
 *   the annotated class
 * @param constructor
 *   the public constructor with the inner value as its only parameter
 * @param accessor
 *   the public method that returns the inner value
 */
final case class ValueTypeDefinition(valueType: Class[?], constructor: Constructor[?], accessor: Method) {

  /** The class of the inner value, for example `int` for `Port(value: Int)`. */
  def innerClass: Class[?] =
    constructor.getParameterTypes.head

  /** The type of the inner value with its type arguments, for example `List<String>` for `Tags(value: List[String])`. */
  def innerType: Type =
    constructor.getGenericParameterTypes.head

  /**
   * Creates the value type from `inner`.
   *
   * @throws dev.coilix.config.valuetype.ValueCreationException
   *   if the constructor rejects `inner`
   */
  def create(inner: AnyRef): AnyRef =
    Try(constructor.newInstance(inner).asInstanceOf[AnyRef]).fold(failure => throw rejected(inner, failure), identity)

  /** The inner value of `value`, which must be an instance of [[valueType]]. */
  def extract(value: AnyRef): AnyRef =
    accessor.invoke(value)

  private def rejected(inner: AnyRef, failure: Throwable): ValueCreationException = {
    val cause = rootOf(failure)
    ValueCreationException(s"Cannot create ${valueType.getName} from '$inner': ${cause.getMessage}", cause)
  }

  private def rootOf(failure: Throwable): Throwable =
    failure match {
      case invocation: InvocationTargetException => Option(invocation.getCause).getOrElse(invocation)
      case other => other
    }
}
