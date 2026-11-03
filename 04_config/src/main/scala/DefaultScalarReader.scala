package dev.coilix.config

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

/**
 * Reads the scalar types supported by Coilix: `String`, `Int`, `Long`, `Double` and `Boolean` (also in their boxed Java
 * forms), `java.time.Duration` and `scala.concurrent.duration.FiniteDuration`.
 */
final class DefaultScalarReader extends ScalarReader {

  private type Read = (ConfigTree, String) => AnyRef

  private val readers: Map[Class[?], Read] = Map(
    classOf[String]             -> ((tree, path) => tree.string(path)),
    classOf[Int]                -> ((tree, path) => Int.box(tree.int(path))),
    classOf[java.lang.Integer]  -> ((tree, path) => Int.box(tree.int(path))),
    classOf[Long]               -> ((tree, path) => Long.box(tree.long(path))),
    classOf[java.lang.Long]     -> ((tree, path) => Long.box(tree.long(path))),
    classOf[Double]             -> ((tree, path) => Double.box(tree.double(path))),
    classOf[java.lang.Double]   -> ((tree, path) => Double.box(tree.double(path))),
    classOf[Boolean]            -> ((tree, path) => Boolean.box(tree.boolean(path))),
    classOf[java.lang.Boolean]  -> ((tree, path) => Boolean.box(tree.boolean(path))),
    classOf[java.time.Duration] -> ((tree, path) => tree.duration(path)),
    classOf[FiniteDuration]     -> ((tree, path) => tree.duration(path).toScala)
  )

  override def supports(targetType: Class[?]): Boolean =
    readers.contains(targetType)

  override def read(tree: ConfigTree, path: String, targetType: Class[?]): AnyRef =
    readers.get(targetType).map(_(tree, path)).getOrElse(throw unsupported(path, targetType))

  private def unsupported(path: String, targetType: Class[?]): ConfigurationException =
    ConfigurationException(s"Cannot read configuration '$path' as ${targetType.getName}")
}
