package dev.coilix.boot

import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.scan.CustomScanApplication
import munit.FunSuite

final class CoilixTest extends FunSuite {

  test("runs an application from its declared scan packages") {
    val context = Coilix.run[CustomScanApplication.type](Array("--name=Ann"))
    try {
      assertEquals(context.getBean[Clock].now, 42L)
      assertEquals(context.getBean[ApplicationArguments].optionValue("name"), Some("Ann"))
    } finally {
      context.close()
    }
  }
}
