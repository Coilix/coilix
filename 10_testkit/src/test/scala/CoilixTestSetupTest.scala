package dev.coilix.testkit

import dev.coilix.boot.BasePackageResolver
import dev.coilix.testkit.fixture.FixtureApplication
import dev.coilix.testkit.fixture.Greeter
import dev.coilix.testkit.fixture.Journal
import munit.FunSuite
import scala.reflect.ClassTag

final class CoilixTestSetupTest extends FunSuite {
  private object NoPackages extends BasePackageResolver {
    override def resolve[T: ClassTag]: List[String] = Nil
  }

  private val base = CoilixTestSetup.forApplication[FixtureApplication.type]

  test("starts without properties, profiles, beans, arguments and runners") {
    assertEquals((base.properties, base.profiles, base.beans, base.arguments, base.runners), (Map.empty, Nil, Nil, Nil, false))
  }

  test("a later property value wins") {
    assertEquals(base.withProperties("a" -> "1").withProperties("a" -> "2").properties, Map("a" -> "2"))
  }

  test("passes the profiles as coilix.profiles.active") {
    assertEquals(base.withProfiles("dev", "local").configurationOverrides, Map("coilix.profiles.active" -> "dev,local"))
  }

  test("without profiles passes only the properties") {
    assertEquals(base.withProperties("a" -> "1").configurationOverrides, Map("a" -> "1"))
  }

  test("names a replaced bean after its type") {
    val journal = Journal()
    assertEquals(base.replaceBean[Journal](journal).beans, List(TestBean("journal", classOf[Journal], journal, replaces = true)))
  }

  test("adds a bean without replacing") {
    val greeter: Greeter = name => name
    assertEquals(base.addBean[Greeter](greeter).beans.map(bean => (bean.beanName, bean.replaces)), List(("greeter", false)))
  }

  test("each change returns a new setup and keeps the original") {
    val changed = base.withRunners.withArguments("--a=1")
    assertEquals((base.runners, base.arguments, changed.runners, changed.arguments), (false, Nil, true, List("--a=1")))
  }

  test("scans the given packages") {
    assertEquals(CoilixTestSetup.forPackages("a.b", "c.d").packages(NoPackages), List("a.b", "c.d"))
  }
}
