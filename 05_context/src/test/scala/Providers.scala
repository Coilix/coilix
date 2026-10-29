package io.github.coilix.context

import com.typesafe.config.ConfigFactory
import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.Environment
import io.github.coilix.config.TypesafeConfigTree

object Providers {
  def fromHocon(hocon: String): ComponentProvider =
    ComponentProvider.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))
}
