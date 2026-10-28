package org.scaling.boot

import java.net.URL
import scala.io.Source
import scala.util.Using

final class ClasspathBannerLoader(resource: String, classLoader: ClassLoader) extends BannerLoader {

  override def load(): Option[String] =
    Option(classLoader.getResource(resource)).map(read)

  private def read(url: URL): String =
    Using.resource(Source.fromURL(url, "UTF-8"))(_.mkString)
}
