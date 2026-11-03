package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Loads the banner printed when the application starts. */
trait BannerLoader {

  /** The banner text, or `None` when there is no banner. */
  def load(): Option[String]
}

/** Configuration path and default implementation of [[dev.coilix.boot.BannerLoader]]. */
object BannerLoader {

  /** Replace the component in the configuration under `coilix.boot.banner-loader`, with a `class` key naming the implementation. */
  given ConfigPath[BannerLoader] = ConfigPath[BannerLoader]("coilix.boot.banner-loader")

  /**
   * The configured implementation, or [[dev.coilix.boot.ClasspathBannerLoader]] when `coilix.boot.banner-loader` is not set. The
   * default reads `banner.txt` from the class path.
   */
  def default(using components: ComponentProvider): BannerLoader =
    components.get[BannerLoader](ClasspathBannerLoader("banner.txt", Thread.currentThread().getContextClassLoader))
}
