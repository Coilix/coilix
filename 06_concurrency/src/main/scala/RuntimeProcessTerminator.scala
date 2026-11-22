package dev.coilix.concurrency

/**
 * A [[dev.coilix.concurrency.ProcessTerminator]] that ends the process through `runtime`. This is the type of the
 * `processTerminator` bean of the concurrency module.
 *
 * @param runtime
 *   the runtime of the JVM
 */
final class RuntimeProcessTerminator(runtime: Runtime) extends ProcessTerminator {

  override def exit(code: Int): Unit = runtime.exit(code)

  override def halt(code: Int): Unit = runtime.halt(code)
}
