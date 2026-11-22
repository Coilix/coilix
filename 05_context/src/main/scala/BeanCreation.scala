package dev.coilix.context

/** When the context creates the instances of a bean. */
enum BeanCreation {

  /** One instance, created when the context starts. The default. */
  case Eager

  /** One instance, created when it is first needed, from [[dev.coilix.annotation.Lazy]]. */
  case Lazy

  /** A new instance for every lookup, from [[dev.coilix.annotation.Prototype]]. */
  case Prototype

  /** Whether the context creates the bean while it starts. */
  def atStartup: Boolean = this == Eager
}
