package io.github.coilix.boot

import java.util.concurrent.atomic.AtomicInteger
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class Slf4jStartupLoggerTest extends FunSuite {

  test("loads the banner when starting") {
    val loads  = AtomicInteger()
    val banner = new BannerLoader {
      override def load(): Option[String] = {
        val _ = loads.incrementAndGet()
        Some("banner")
      }
    }
    Slf4jStartupLogger(banner, DefaultStartupMessageFormatter()).starting("DemoApplication")
    assertEquals(loads.get, 1)
  }

  test("logs the startup time without loading the banner again") {
    val banner = new BannerLoader {
      override def load(): Option[String] = fail("banner loaded after startup")
    }
    Slf4jStartupLogger(banner, DefaultStartupMessageFormatter()).started("DemoApplication", 5.millis)
  }
}
