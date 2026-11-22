package dev.coilix.context

/**
 * Gives access to a bean of type `T` when it is needed instead of when the bean that declares it is created.
 *
 * A constructor parameter of type `Provider[T]` does not make its bean wait for `T`, so it breaks a dependency cycle, and it is
 * the way to get a new instance of a [[dev.coilix.annotation.Prototype]] bean each time. Building the context fails when no bean
 * provides `T`, but `T` is created only by `get()`, and a [[dev.coilix.annotation.Lazy]] bean only then:
 *
 * ```scala
 * @Service
 * final class Checkout(carts: Provider[ShoppingCart]) {
 *   def start(): ShoppingCart = carts.get()
 * }
 * ```
 *
 * @tparam T
 *   the type of the bean
 */
trait Provider[T] {

  /**
   * The bean of type `T`: the shared instance of a singleton, or a new instance of a prototype.
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if the bean is not created yet because it is needed while the context is still starting
   */
  def get(): T
}
