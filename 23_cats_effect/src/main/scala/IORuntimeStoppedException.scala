package dev.coilix.catseffect

/**
 * Reported to the [[dev.coilix.concurrency.FatalErrorHandler]] when cats-effect shuts its runtime down by itself, which it does
 * after a fatal error such as an `OutOfMemoryError` in a fiber. cats-effect prints the original error before.
 *
 * It is a `VirtualMachineError`, so it counts as fatal, like the error that caused it.
 */
final class IORuntimeStoppedException
    extends VirtualMachineError("The cats-effect runtime shut down after a fatal error in a fiber, printed above by cats-effect")
