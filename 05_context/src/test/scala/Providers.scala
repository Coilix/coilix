package dev.coilix.context

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree

object Providers {
  def fromHocon(hocon: String): ComponentProvider =
    ComponentProvider.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))
}
