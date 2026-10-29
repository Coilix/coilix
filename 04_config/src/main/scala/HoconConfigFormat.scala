package io.github.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax
import java.net.URL

final class HoconConfigFormat extends ConfigFormat {

  override def extensions: List[String] = List("conf")

  override def parse(url: URL): Config =
    ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF))
}
