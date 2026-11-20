package dev.coilix.context

/**
 * A proxy registered in place of a bean, which gives access to the instance behind it.
 *
 * `@PostConstruct` and `@PreDestroy` methods, and other methods found on the bean class, are invoked on the instance behind
 * the proxy, because the proxy implements only the traits of the bean.
 */
trait ProxiedBean {

  /** The instance behind the proxy. */
  def proxiedBean: AnyRef
}

/** Access to the instance behind a [[dev.coilix.context.ProxiedBean]]. */
object ProxiedBean {

  /** The instance behind `bean` when it is a proxy, otherwise `bean` itself. */
  def unwrap(bean: AnyRef): AnyRef =
    bean match {
      case proxy: ProxiedBean => proxy.proxiedBean
      case other => other
    }
}
