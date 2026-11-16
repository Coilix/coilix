package dev.coilix.demo
package access

/** A workshop key that a vehicle trusts for checking token signatures. */
final case class PublicKey(value: java.security.PublicKey)
