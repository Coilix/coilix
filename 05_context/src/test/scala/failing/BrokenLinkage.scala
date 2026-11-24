package dev.coilix.context
package failing

/** A class whose constructor throws a fatal error. */
final class BrokenLinkage {
  throw LinkageError("a library is missing")
}
