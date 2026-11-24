package dev.coilix.demo
package workshop

/**
 * The recall bulletin of the manufacturers, downloaded once when the workshop opens.
 *
 * @param issue
 *   the issue of the bulletin, such as `2026-11`
 * @param portal
 *   the address of the portal it came from
 */
final case class RecallBulletin(issue: String, portal: String)
