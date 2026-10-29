package io.github.coilix.config

import com.typesafe.config.ConfigFactory

object ConfigTrees {
  def parse(hocon: String): ConfigTree = TypesafeConfigTree(ConfigFactory.parseString(hocon))
}
