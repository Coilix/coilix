package dev.coilix.config
package fixture

import dev.coilix.config.enums.Mode
import dev.coilix.config.valuetype.Port
import dev.coilix.config.valuetype.Secret
import dev.coilix.config.valuetype.Tags

final case class ServerSettings(port: Port, mode: Mode, apiKey: Secret, modes: List[Mode], tags: Tags, backup: Option[Port])
