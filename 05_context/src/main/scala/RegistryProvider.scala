package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * The [[dev.coilix.context.Provider]] injected for a [[dev.coilix.context.Dependency.Deferred]] dependency, which looks the bean
 * up in the complete registry of the context on every `get()`.
 *
 * @param registry
 *   the registry at the moment the depending bean was created, whose [[dev.coilix.context.BeanRegistry.complete]] is the
 *   registry of the started context
 * @param beanType
 *   the type of the bean
 * @param requiredBy
 *   the name of the bean that declares the provider, used in error messages
 */
case class RegistryProvider[T](registry: BeanRegistry, beanType: GenericType, requiredBy: String) extends Provider[T] {

  override def get(): T =
    registry.complete.find(beanType).map(bean => typed(bean.instance)).getOrElse(throw missing)

  /**
   * `instance` as a `T`. The cast is unchecked, because `T` is erased, but the registry found `instance` by the full type of
   * `T`.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def typed(instance: AnyRef): T = instance.asInstanceOf[T]

  private def missing: NoSuchBeanException =
    new NoSuchBeanException(
      s"Provider[$beanType] of bean '$requiredBy' found no bean. A provider used while the context is starting sees only the beans " +
        "created before the bean that declares it"
    )
}
