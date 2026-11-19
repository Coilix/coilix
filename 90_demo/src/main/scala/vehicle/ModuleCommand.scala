package dev.coilix.demo
package vehicle

/**
 * One command for a control module.
 *
 * @param parameters
 *   values the action needs, such as `key -> 2` when a new key is programmed
 */
final case class ModuleCommand(action: Action, requestId: RequestId, parameters: Map[String, String] = Map.empty)
