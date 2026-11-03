package dev.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax
import java.net.URL

/** Reads Java properties files with the `.properties` extension. Dotted keys become nested sections. */
final class PropertiesConfigFormat extends ConfigFormat {

  override def extensions: List[String] = List("properties")

  override def parse(url: URL): Config =
    ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES))
}
