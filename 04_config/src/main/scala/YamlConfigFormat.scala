package dev.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.net.URL
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import scala.jdk.CollectionConverters.*
import scala.util.Using

final class YamlConfigFormat extends ConfigFormat {

  override def extensions: List[String] = List("yaml", "yml")

  override def parse(url: URL): Config =
    Using.resource(url.openStream())(stream => toConfig(url, Load(LoadSettings.builder().build()).loadFromInputStream(stream)))

  private def toConfig(url: URL, document: Any): Config =
    document match {
      case root: java.util.Map[?, ?] => ConfigFactory.parseMap(stringKeys(root), url.toString)
      case _ => ConfigFactory.empty(url.toString)
    }

  private def stringKeys(map: java.util.Map[?, ?]): java.util.Map[String, AnyRef] =
    map.asScala.toList.collect { case (key, value: AnyRef) => String.valueOf(key) -> normalized(value) }.toMap.asJava

  private def normalized(value: AnyRef): AnyRef =
    value match {
      case nested: java.util.Map[?, ?] => stringKeys(nested)
      case list:   java.util.List[?]   => list.asScala.toList.collect { case element: AnyRef => normalized(element) }.asJava
      case scalar => scalar
    }
}
