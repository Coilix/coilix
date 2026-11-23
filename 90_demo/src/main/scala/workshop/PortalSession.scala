package dev.coilix.demo
package workshop

import dev.coilix.annotation.Slf4s
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The logged-in session of the workshop at the manufacturer portal, opened when the workshop opens and closed when it closes.
 *
 * It is created by the `Resource` of [[dev.coilix.demo.workshop.PortalConfiguration.portalSession]].
 *
 * @param url
 *   the address of the portal
 */
@Slf4s
final class PortalSession(val url: String) {
  private val open = AtomicBoolean(false)

  /** Whether the workshop is logged in. */
  def isOpen: Boolean = open.get

  /** Logs the workshop in. */
  def logIn(): PortalSession = {
    open.set(true)
    log.info(s"Logged in to the manufacturer portal at $url")
    this
  }

  /** Logs the workshop out. */
  def logOut(): Unit = {
    open.set(false)
    log.info(s"Logged out of the manufacturer portal at $url")
  }
}
