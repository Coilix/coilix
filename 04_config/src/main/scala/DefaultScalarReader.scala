package dev.coilix.config

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

/**
 * Reads the scalar types supported by Coilix: `String`, `Int`, `Long`, `Double` and `Boolean` (also in their boxed Java
 * forms), `java.time.Duration` and `scala.concurrent.duration.FiniteDuration`.
 */
final class DefaultScalarReader extends ScalarReader {

  private type Read = (ConfigTree, String) => Either[ConfigError, AnyRef]

  private val readers: Map[Class[?], Read] = Map(
    classOf[String]             -> ((tree, path) => tree.string(path)),
    classOf[Int]                -> ((tree, path) => tree.int(path).map(Int.box)),
    classOf[java.lang.Integer]  -> ((tree, path) => tree.int(path).map(Int.box)),
    classOf[Long]               -> ((tree, path) => tree.long(path).map(Long.box)),
    classOf[java.lang.Long]     -> ((tree, path) => tree.long(path).map(Long.box)),
    classOf[Double]             -> ((tree, path) => tree.double(path).map(Double.box)),
    classOf[java.lang.Double]   -> ((tree, path) => tree.double(path).map(Double.box)),
    classOf[Boolean]            -> ((tree, path) => tree.boolean(path).map(Boolean.box)),
    classOf[java.lang.Boolean]  -> ((tree, path) => tree.boolean(path).map(Boolean.box)),
    classOf[java.time.Duration] -> ((tree, path) => tree.duration(path)),
    classOf[FiniteDuration]     -> ((tree, path) => tree.duration(path).map(_.toScala))
  )

  override def supports(targetType: Class[?]): Boolean =
    readers.contains(targetType)

  override def read(tree: ConfigTree, path: String, targetType: Class[?]): Either[ConfigError, AnyRef] =
    readers.get(targetType).toRight(ConfigError.UnsupportedScalar(path, targetType)).flatMap(_(tree, path))
}
