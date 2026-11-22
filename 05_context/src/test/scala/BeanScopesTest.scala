package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.scopes.CartHolder
import dev.coilix.context.scopes.Checkout
import dev.coilix.context.scopes.Chicken
import dev.coilix.context.scopes.CreationJournal
import dev.coilix.context.scopes.Egg
import dev.coilix.context.scopes.Orphan
import dev.coilix.context.scopes.ReportArchive
import dev.coilix.context.scopes.ShoppingCart
import dev.coilix.context.scopes.Ticket
import dev.coilix.context.scopes.TicketConfiguration
import munit.FunSuite

final class BeanScopesTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("reads the creation of a bean from @Prototype and @Lazy on the class and on a @Bean method") {
    assertEquals(
      Definitions
        .of(classOf[ShoppingCart], classOf[ReportArchive], classOf[CreationJournal], classOf[TicketConfiguration])
        .map(definition => definition.beanName -> definition.creation)
        .toMap,
      Map(
        "shoppingCart"        -> BeanCreation.Prototype,
        "reportArchive"       -> BeanCreation.Lazy,
        "creationJournal"     -> BeanCreation.Eager,
        "ticketConfiguration" -> BeanCreation.Eager,
        "ticket"              -> BeanCreation.Prototype
      )
    )
  }

  test("creates a new prototype for every lookup and never destroys it") {
    val context = contextOf(classOf[ShoppingCart])
    val journal = context.getBean[CreationJournal]
    val atStart = journal.all
    val first   = context.getBean[ShoppingCart]
    val second  = context.getBean[ShoppingCart]
    context.close()
    assertEquals((atStart, first eq second, journal.all), (Nil, false, List("cart created", "cart created")))
  }

  test("creates a prototype from a @Bean method for every lookup") {
    val context = contextOf(classOf[TicketConfiguration])
    assertEquals(List(context.getBean[Ticket], context.getBean[Ticket]).map(_.number), List(1, 2))
  }

  test("creates a lazy singleton on first use, keeps it and destroys it with the context") {
    val context = contextOf(classOf[ReportArchive])
    val journal = context.getBean[CreationJournal]
    val atStart = journal.all
    val same    = context.getBean[ReportArchive] eq context.getBean[ReportArchive]
    context.close()
    assertEquals((atStart, same, journal.all), (Nil, true, List("archive created", "archive closed")))
  }

  test("does not destroy a lazy singleton that was never created") {
    val context = contextOf(classOf[ReportArchive])
    context.close()
    assertEquals(context.getBean[CreationJournal].all, Nil)
  }

  test("lists every bean by name but only the created ones as created beans") {
    val context = contextOf(classOf[ShoppingCart], classOf[ReportArchive])
    val names   = context.beanNames.filter(name => name == "shoppingCart" || name == "reportArchive")
    val before  = context.createdBeans.map(_.beanName).filter(names.contains)
    val _       = (context.getBean[ShoppingCart], context.getBean[ReportArchive])
    val after   = context.createdBeans.map(_.beanName).filter(names.contains)
    assertEquals((names, before, after), (List("shoppingCart", "reportArchive"), Nil, List("reportArchive")))
  }

  test("a provider gives a new prototype on every get") {
    val context  = contextOf(classOf[ShoppingCart], classOf[Checkout])
    val checkout = context.getBean[Checkout]
    assert(!(checkout.start() eq checkout.start()))
  }

  test("a provider breaks a dependency cycle") {
    val context = contextOf(classOf[Chicken], classOf[Egg])
    val chicken = context.getBean[Chicken]
    assert(chicken.egg.get().chicken eq chicken)
  }

  test("a provider of a type that no bean provides fails the startup") {
    intercept[NoSuchBeanException](contextOf(classOf[Orphan]))
  }

  test("a singleton that requires a prototype directly keeps one instance of it") {
    val context = contextOf(classOf[ShoppingCart], classOf[CartHolder])
    assertEquals(
      (context.getBean[CartHolder].cart eq context.getBean[CartHolder].cart, context.getBean[CreationJournal].all.size),
      (true, 1)
    )
  }

  private def contextOf(classes: Class[?]*): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(Definitions.of((classOf[CreationJournal] +: classes)*))
}
