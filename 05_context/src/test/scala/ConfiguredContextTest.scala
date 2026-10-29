package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.context.configured.Salutation
import io.github.coilix.context.configured.ServerSettings
import io.github.coilix.context.configured.Signature
import io.github.coilix.context.configured.ValueBean
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ConfiguredContextTest extends FunSuite {
  private given ComponentProvider = Providers.fromHocon("server { host = example.org }, salutation.word = Hi")

  private val context =
    ApplicationContextBuilder.default.build(ClassPathScanner.default.scan("io.github.coilix.context.configured"))

  test("binds a @ConfigurationProperties bean with defaults") {
    assertEquals(context.getBean[ServerSettings], ServerSettings("example.org", 80))
  }

  test("injects @Value parameters with defaults and options") {
    val bean = context.getBean[ValueBean]
    assertEquals((bean.host, bean.timeout, bean.debug), ("example.org", 5.seconds, None))
  }

  test("creates beans from @Bean methods") {
    assertEquals(context.getBean[Salutation].salute("Ann"), "Hi Ann")
  }

  test("injects other beans into @Bean methods") {
    assertEquals(context.getBean[Signature].text, "Hi team from example.org")
  }

  test("names @Bean beans after the method or the annotation") {
    assertEquals(
      context.beanNames.sorted,
      List("applicationContext", "companySignature", "salutation", "salutationConfiguration", "serverSettings", "valueBean")
    )
  }
}
