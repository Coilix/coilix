package dev.coilix.config
package valuetype

import dev.coilix.annotation.ValueType

@ValueType
final case class Tags(value: List[String])
