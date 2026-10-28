package org.scaling.boot

import munit.FunSuite
import org.scaling.boot.application.FixtureApplication
import org.scaling.boot.scan.CustomScanApplication
import org.scaling.boot.scan.NotAnnotatedApplication

final class AnnotationBasePackageResolverTest extends FunSuite {
  private val resolver = AnnotationBasePackageResolver()

  test("uses the package of the annotated object") {
    assertEquals(resolver.resolve[FixtureApplication.type], List("org.scaling.boot.application"))
  }

  test("uses the declared scan base packages") {
    assertEquals(resolver.resolve[CustomScanApplication.type], List("org.scaling.boot.fixture", "org.scaling.boot.empty"))
  }

  test("rejects a class without the annotation") {
    intercept[NotABootApplicationException](resolver.resolve[NotAnnotatedApplication.type])
  }
}
