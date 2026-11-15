package dev.coilix.config
package valuetype

import dev.coilix.annotation.ValueType

@ValueType
final class Hidden(value: Int) {
  def doubled: Int = value * 2
}
