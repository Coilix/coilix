package com.example

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import java.time.Clock

class ProfilesExampleTest extends munit.FunSuite {

  private val classes = List(classOf[ConsoleMailSender], classOf[SmtpMailSender], classOf[AuditLog], classOf[ClockConfiguration])

  private def contextOf(hocon: String): ApplicationContext = {
    given ComponentProvider = ComponentProvider.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))
    ApplicationContextBuilder.default.build(classes.flatMap(BeanDefinitionFactory.default.create))
  }

  test("the dev profile selects the console sender") {
    assertEquals(contextOf("coilix.profiles.active = dev").getBean[MailSender].getClass, classOf[ConsoleMailSender])
  }

  test("without the dev profile the SMTP sender is used") {
    assertEquals(contextOf("mail.host = smtp.example.com").getBean[MailSender].getClass, classOf[SmtpMailSender])
  }

  test("the audit log needs shop.audit.enabled = true") {
    assertEquals(contextOf("coilix.profiles.active = dev, shop.audit.enabled = true").getBeansOfType[AuditLog].size, 1)
    assertEquals(contextOf("coilix.profiles.active = dev").getBeansOfType[AuditLog], Nil)
  }

  test("the default clock is added when the application has none") {
    assertEquals(contextOf("coilix.profiles.active = dev").getBeansOfType[Clock].size, 1)
  }
}
