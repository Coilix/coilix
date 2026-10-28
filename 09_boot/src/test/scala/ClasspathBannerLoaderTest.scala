package org.scaling.boot

import munit.FunSuite

final class ClasspathBannerLoaderTest extends FunSuite {
  private val classLoader = getClass.getClassLoader

  test("loads the banner from the classpath") {
    assertEquals(ClasspathBannerLoader("test-banner.txt", classLoader).load().map(_.trim), Some("test banner"))
  }

  test("returns none when the banner is missing") {
    assertEquals(ClasspathBannerLoader("missing-banner.txt", classLoader).load(), None)
  }
}
