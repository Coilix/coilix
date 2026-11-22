package dev.coilix.concurrency

/**
 * Ends the JVM process. The concurrency module registers it as the `processTerminator` bean, so tests replace it instead of
 * stopping the JVM.
 */
trait ProcessTerminator {

  /** Ends the process with `code` after running the shutdown hooks, like `System.exit`. */
  def exit(code: Int): Unit

  /** Ends the process with `code` at once, without running the shutdown hooks, like `Runtime.halt`. */
  def halt(code: Int): Unit
}
