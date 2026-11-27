package dev.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigUtil
import com.typesafe.config.ConfigValueType
import java.time.Duration
import scala.jdk.CollectionConverters.*

/**
 * A [[dev.coilix.config.ConfigTree]] backed by a Typesafe Config `Config`.
 *
 * Errors from Typesafe Config are returned as [[dev.coilix.config.ConfigError.Unreadable]] with the path that was read.
 */
case class TypesafeConfigTree(config: Config) extends ConfigTree {

  override def contains(path: String): Boolean =
    read(path)(config.hasPath(path)).getOrElse(false)

  override def isSection(path: String): Boolean =
    contains(path) && config.getValue(path).valueType == ConfigValueType.OBJECT

  override def keys(path: String): Either[ConfigError, List[String]] =
    read(path)(config.getConfig(path).root().keySet().asScala.toList.sorted)

  override def elements(path: String): Either[ConfigError, List[ConfigTree]] =
    read(path)(config.getList(path).asScala.toList.map(value => TypesafeConfigTree(value.atKey(ConfigTree.elementKey))))

  override def child(path: String, key: String): String =
    ConfigUtil.joinPath((ConfigUtil.splitPath(path).asScala.toList :+ key).asJava)

  override def string(path: String): Either[ConfigError, String] = read(path)(config.getString(path))

  override def int(path: String): Either[ConfigError, Int] = read(path)(config.getInt(path))

  override def long(path: String): Either[ConfigError, Long] = read(path)(config.getLong(path))

  override def double(path: String): Either[ConfigError, Double] = read(path)(config.getDouble(path))

  override def boolean(path: String): Either[ConfigError, Boolean] = read(path)(config.getBoolean(path))

  override def duration(path: String): Either[ConfigError, Duration] = read(path)(config.getDuration(path))

  override def anyRef(path: String): Either[ConfigError, AnyRef] = read(path)(config.getAnyRef(path))

  override def withFallback(values: Map[String, String]): ConfigTree =
    TypesafeConfigTree(config.withFallback(ConfigFactory.parseMap(values.asJava)))

  private def read[A](path: String)(value: => A): Either[ConfigError, A] =
    try {
      Right(value)
    } catch {
      case error: ConfigException => Left(ConfigError.Unreadable(path, error.getMessage))
    }
}
