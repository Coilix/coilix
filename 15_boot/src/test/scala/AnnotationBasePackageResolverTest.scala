package dev.coilix.boot

import dev.coilix.boot.application.FixtureApplication
import dev.coilix.boot.scan.CustomScanApplication
import dev.coilix.boot.scan.NotAnnotatedApplication
import munit.FunSuite

final class AnnotationBasePackageResolverTest extends FunSuite {
  private val resolver = AnnotationBasePackageResolver()

  test("uses the package of the annotated object") {
    assertEquals(resolver.resolve[FixtureApplication.type], Right(List("dev.coilix.boot.application")))
  }

  test("uses the declared scan base packages") {
    assertEquals(resolver.resolve[CustomScanApplication.type], Right(List("dev.coilix.boot.fixture", "dev.coilix.boot.empty")))
  }

  test("rejects a class without the annotation") {
    assertEquals(
      resolver.resolve[NotAnnotatedApplication.type],
      Left(StartupError.NotABootApplication(classOf[NotAnnotatedApplication.type]))
    )
  }
}
