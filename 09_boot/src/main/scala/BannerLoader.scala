package io.github.coilix.boot

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait BannerLoader {
  def load(): Option[String]
}

object BannerLoader {
  given ConfigPath[BannerLoader] = ConfigPath[BannerLoader]("coilix.boot.banner-loader")

  def default(using components: ComponentProvider): BannerLoader =
    components.get[BannerLoader](ClasspathBannerLoader("banner.txt", Thread.currentThread().getContextClassLoader))
}
