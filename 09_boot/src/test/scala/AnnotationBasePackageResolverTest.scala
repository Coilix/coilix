package io.github.coilix.boot

import io.github.coilix.boot.application.FixtureApplication
import io.github.coilix.boot.scan.CustomScanApplication
import io.github.coilix.boot.scan.NotAnnotatedApplication
import munit.FunSuite

final class AnnotationBasePackageResolverTest extends FunSuite {
  private val resolver = AnnotationBasePackageResolver()

  test("uses the package of the annotated object") {
    assertEquals(resolver.resolve[FixtureApplication.type], List("io.github.coilix.boot.application"))
  }

  test("uses the declared scan base packages") {
    assertEquals(resolver.resolve[CustomScanApplication.type], List("io.github.coilix.boot.fixture", "io.github.coilix.boot.empty"))
  }

  test("rejects a class without the annotation") {
    intercept[NotABootApplicationException](resolver.resolve[NotAnnotatedApplication.type])
  }
}
