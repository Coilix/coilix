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
 * Errors from Typesafe Config are rethrown as [[dev.coilix.config.ConfigurationException]] with the path that was read.
 */
final class TypesafeConfigTree(config: Config) extends ConfigTree {

  override def contains(path: String): Boolean =
    guarded(path)(config.hasPath(path))

  override def isSection(path: String): Boolean =
    contains(path) && config.getValue(path).valueType == ConfigValueType.OBJECT

  override def keys(path: String): List[String] =
    guarded(path)(config.getConfig(path).root().keySet().asScala.toList.sorted)

  override def elements(path: String): List[ConfigTree] =
    guarded(path)(config.getList(path).asScala.toList.map(value => TypesafeConfigTree(value.atKey(ConfigTree.elementKey))))

  override def child(path: String, key: String): String =
    ConfigUtil.joinPath((ConfigUtil.splitPath(path).asScala.toList :+ key).asJava)

  override def string(path: String): String = guarded(path)(config.getString(path))

  override def int(path: String): Int = guarded(path)(config.getInt(path))

  override def long(path: String): Long = guarded(path)(config.getLong(path))

  override def double(path: String): Double = guarded(path)(config.getDouble(path))

  override def boolean(path: String): Boolean = guarded(path)(config.getBoolean(path))

  override def duration(path: String): Duration = guarded(path)(config.getDuration(path))

  override def anyRef(path: String): AnyRef = guarded(path)(config.getAnyRef(path))

  override def withFallback(values: Map[String, String]): ConfigTree =
    TypesafeConfigTree(config.withFallback(ConfigFactory.parseMap(values.asJava)))

  private def guarded[A](path: String)(read: => A): A =
    try {
      read
    } catch {
      case error: ConfigException => throw ConfigurationException(s"Cannot read configuration '$path': ${error.getMessage}")
    }
}
