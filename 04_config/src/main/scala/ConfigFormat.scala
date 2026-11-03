package dev.coilix.config

import com.typesafe.config.Config
import java.net.URL

/** A configuration file format that Coilix can read. */
trait ConfigFormat {

  /** File extensions of this format, without the dot, in the order they are looked up. */
  def extensions: List[String]

  /**
   * Reads the file at `url` into a Typesafe Config `Config`.
   *
   * @throws com.typesafe.config.ConfigException
   *   if the file is not valid in this format
   */
  def parse(url: URL): Config
}

/** Built-in configuration formats. */
object ConfigFormat {

  /**
   * Properties, YAML and HOCON formats, from the highest to the lowest precedence.
   *
   * When an application has several configuration files, a key in `application.properties` wins over the same key in
   * `application.yaml`, `application.yml` and `application.conf`.
   */
  def byPrecedence: List[ConfigFormat] =
    List(PropertiesConfigFormat(), YamlConfigFormat(), HoconConfigFormat())
}
