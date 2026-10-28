package org.scaling.context

import com.typesafe.config.ConfigFactory
import org.scaling.config.ComponentProvider
import org.scaling.config.Environment
import org.scaling.config.TypesafeConfigTree

object Providers {
  def fromHocon(hocon: String): ComponentProvider =
    ComponentProvider.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))
}
