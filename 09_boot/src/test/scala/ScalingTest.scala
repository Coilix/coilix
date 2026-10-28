package org.scaling.boot

import munit.FunSuite
import org.scaling.boot.fixture.Clock
import org.scaling.boot.scan.CustomScanApplication

final class ScalingTest extends FunSuite {

  test("runs an application from its declared scan packages") {
    val context = Scaling.run[CustomScanApplication.type](Array("--name=Ann"))
    try {
      assertEquals(context.getBean[Clock].now, 42L)
      assertEquals(context.getBean[ApplicationArguments].optionValue("name"), Some("Ann"))
    } finally {
      context.close()
    }
  }
}
