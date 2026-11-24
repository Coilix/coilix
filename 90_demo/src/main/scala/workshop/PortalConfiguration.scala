package dev.coilix.demo
package workshop

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.ShutdownTimeout

/**
 * Beans of the manufacturer portal made by cats-effect: other beans receive the session and the bulletin, not the effects.
 *
 * The session is opened before the bulletin is downloaded, because the bulletin needs it, and closed after every bean that uses
 * it. Logging out may take at most `workshop.oem-portal.logout-timeout`.
 */
@Configuration
final class PortalConfiguration {

  /** The session at the portal, logged in when the workshop opens and logged out when it closes. */
  @Bean
  @ShutdownTimeout("${workshop.oem-portal.logout-timeout:5s}")
  def portalSession(properties: WorkshopProperties): Resource[IO, PortalSession] =
    Resource.make(IO.blocking(PortalSession(properties.oemPortal.url).logIn()))(session => IO.blocking(session.logOut()))

  /** The current recall bulletin, downloaded with the session when the workshop opens. */
  @Bean
  def recallBulletin(session: PortalSession): IO[RecallBulletin] =
    IO.blocking(RecallBulletin(issue = "2026-11", portal = session.url))
}
