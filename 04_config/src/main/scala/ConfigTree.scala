package dev.coilix.config

import com.typesafe.config.ConfigFactory
import java.time.Duration

/**
 * Read access to a tree of configuration values.
 *
 * Paths use dots to separate keys, as in `shop.mail.host`. Every read method returns a
 * [[dev.coilix.config.ConfigError.Unreadable]] when the value is missing or has a different shape.
 */
trait ConfigTree {

  /** Whether `path` has a value, either a single value, a list or a section. A malformed path has no value. */
  def contains(path: String): Boolean

  /** Whether the value at `path` is a section with its own keys. */
  def isSection(path: String): Boolean

  /** The keys directly under the section at `path`, sorted alphabetically. */
  def keys(path: String): Either[ConfigError, List[String]]

  /**
   * The elements of the list at `path`, each as a separate tree that holds the element under
   * [[dev.coilix.config.ConfigTree.elementKey]].
   */
  def elements(path: String): Either[ConfigError, List[ConfigTree]]

  /** The path of `key` under `path`, with `key` quoted when it contains dots or other special characters. */
  def child(path: String, key: String): String

  /** The value at `path` as a string. Numbers and booleans are converted to their text. */
  def string(path: String): Either[ConfigError, String]

  /** The value at `path` as an `Int`. */
  def int(path: String): Either[ConfigError, Int]

  /** The value at `path` as a `Long`. */
  def long(path: String): Either[ConfigError, Long]

  /** The value at `path` as a `Double`. */
  def double(path: String): Either[ConfigError, Double]

  /** The value at `path` as a `Boolean`. Accepts `true`, `false`, `yes`, `no`, `on` and `off`. */
  def boolean(path: String): Either[ConfigError, Boolean]

  /** The value at `path` as a duration, written as `500ms`, `5s`, `2 minutes` or a number of milliseconds. */
  def duration(path: String): Either[ConfigError, Duration]

  /** The value at `path` as a plain Java value: a string, a boxed number or boolean, a `java.util.List` or a `java.util.Map`. */
  def anyRef(path: String): Either[ConfigError, AnyRef]

  /** A tree that uses `values` for paths this tree does not define. */
  def withFallback(values: Map[String, String]): ConfigTree
}

/** Shared values for [[dev.coilix.config.ConfigTree]]. */
object ConfigTree {

  /** The key under which [[dev.coilix.config.ConfigTree.elements]] stores each list element. */
  val elementKey: String = "element"

  /** A tree without any values. */
  def empty: ConfigTree = TypesafeConfigTree(ConfigFactory.empty())
}
