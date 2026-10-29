package io.github.coilix.config

import com.typesafe.config.Config
import java.net.URL

trait ConfigFormat {
  def extensions: List[String]

  def parse(url: URL): Config
}

object ConfigFormat {
  def byPrecedence: List[ConfigFormat] =
    List(PropertiesConfigFormat(), YamlConfigFormat(), HoconConfigFormat())
}
