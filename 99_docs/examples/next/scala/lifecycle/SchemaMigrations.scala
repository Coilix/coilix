package com.example
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct

@Component
class SchemaMigrations(table: PriceTable) {
  @PostConstruct
  private def migrate(): Unit = table.create()
}
