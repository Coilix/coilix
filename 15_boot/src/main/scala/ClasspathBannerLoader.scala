package dev.coilix.boot

import java.net.URL
import scala.io.Source
import scala.util.Using

/**
 * Reads the banner from a UTF-8 class path resource. A missing resource means no banner.
 *
 * This is the default [[dev.coilix.boot.BannerLoader]], reading `banner.txt`.
 *
 * @param resource
 *   path of the resource, for example `banner.txt`
 * @param classLoader
 *   class loader used to find the resource
 */
final class ClasspathBannerLoader(resource: String, classLoader: ClassLoader) extends BannerLoader {

  override def load(): Option[String] =
    Option(classLoader.getResource(resource)).map(read)

  private def read(url: URL): String =
    Using.resource(Source.fromURL(url, "UTF-8"))(_.mkString)
}
