package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait BannerLoader {
  def load(): Option[String]
}

object BannerLoader {
  given ConfigPath[BannerLoader] = ConfigPath[BannerLoader]("scaling.boot.banner-loader")

  def default(using components: ComponentProvider): BannerLoader =
    components.get[BannerLoader](ClasspathBannerLoader("banner.txt", Thread.currentThread().getContextClassLoader))
}
