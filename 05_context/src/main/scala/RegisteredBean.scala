package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.common.TypeInfo

/**
 * A created bean held by a [[dev.coilix.context.BeanRegistry]].
 *
 * @param beanName
 *   unique name of the bean
 * @param beanType
 *   type under which the bean is registered and injected, with its type arguments
 * @param isPrimary
 *   whether the bean wins when several beans match a requested type
 * @param instance
 *   the bean itself
 * @param lifecycle
 *   lifecycle methods of the bean, of which the `@PreDestroy` methods run when the context closes
 */
final case class RegisteredBean(
  beanName:  String,
  beanType:  GenericType,
  isPrimary: Boolean,
  instance:  AnyRef,
  lifecycle: LifecycleMethods = LifecycleMethods.none
) extends BeanDescriptor

/** Factories for [[dev.coilix.context.RegisteredBean]]. */
object RegisteredBean {

  /**
   * A ready instance registered under its full static type `T`, so it matches dependencies with type arguments such as
   * `Repository[IO]`.
   *
   * ```scala
   * RegisteredBean.of[Repository[IO]]("repository", InMemoryRepository())
   * ```
   */
  def of[T <: AnyRef: TypeInfo](beanName: String, instance: T, isPrimary: Boolean = false): RegisteredBean =
    RegisteredBean(beanName, TypeInfo[T].genericType, isPrimary, instance)

  /** A ready instance registered under `beanClass`, without type arguments. */
  def ofClass(beanName: String, beanClass: Class[?], instance: AnyRef): RegisteredBean =
    RegisteredBean(beanName, GenericType.of(beanClass), isPrimary = false, instance)
}
