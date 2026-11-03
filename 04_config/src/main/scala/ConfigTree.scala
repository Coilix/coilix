package dev.coilix.config

import com.typesafe.config.ConfigFactory
import java.time.Duration

trait ConfigTree {
  def contains(path: String): Boolean

  def isSection(path: String): Boolean

  def keys(path: String): List[String]

  def elements(path: String): List[ConfigTree]

  def child(path: String, key: String): String

  def string(path: String): String

  def int(path: String): Int

  def long(path: String): Long

  def double(path: String): Double

  def boolean(path: String): Boolean

  def duration(path: String): Duration

  def anyRef(path: String): AnyRef

  def withFallback(values: Map[String, String]): ConfigTree
}

object ConfigTree {
  val elementKey: String = "element"

  def empty: ConfigTree = TypesafeConfigTree(ConfigFactory.empty())
}
