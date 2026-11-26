package dev.coilix.config

import com.typesafe.config.Config
import java.net.URL
import scala.reflect.ClassTag

/** Shared parsing for the built-in [[dev.coilix.config.ConfigFormat]] implementations. */
private[config] object ConfigFormats {

  /** The result of `parse`, or a [[dev.coilix.config.ConfigError.Unparsable]] for `url` when it throws `E`. */
  def parsed[E <: Throwable: ClassTag](url: URL)(parse: => Config): Either[ConfigError, Config] =
    try {
      Right(parse)
    } catch {
      case error: E => Left(ConfigError.Unparsable(url.toString, error.getMessage))
    }
}
