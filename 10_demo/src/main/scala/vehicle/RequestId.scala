package dev.coilix.demo
package vehicle

/** Identifier of one command sent to a module. A vehicle accepts each identifier once per session. */
final case class RequestId(value: String)
