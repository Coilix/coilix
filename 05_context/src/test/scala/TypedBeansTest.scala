package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.context.typed.GenericRepo
import dev.coilix.context.typed.LambdaRepoUser
import dev.coilix.context.typed.ListRepo
import dev.coilix.context.typed.OptionRepo
import dev.coilix.context.typed.Repo
import dev.coilix.context.typed.RepoUser
import dev.coilix.context.typed.TypedConfiguration
import dev.coilix.context.typed.UnboundRepoUser
import munit.FunSuite

final class TypedBeansTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private def contextOf(classes: Class[?]*): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(Definitions.of(classes*))

  private val repositories = List(classOf[OptionRepo], classOf[ListRepo], classOf[TypedConfiguration])

  test("injects beans that differ only in their type arguments") {
    assertEquals(contextOf((classOf[RepoUser] :: repositories)*).getBean[RepoUser].names, List("option", "list", "vector"))
  }

  test("injects every bean of a type with a wildcard argument") {
    assertEquals(contextOf((classOf[RepoUser] :: repositories)*).getBean[RepoUser].count, 3)
  }

  test("looks up beans by their full type") {
    val context = contextOf(repositories*)
    assertEquals(
      (context.getBean[Repo[Option]].name, context.getBean[Repo[List]].name, context.getBean[Repo[Vector]].name),
      ("option", "list", "vector")
    )
  }

  test("does not match a bean method result with other type arguments") {
    assertEquals(contextOf(classOf[TypedConfiguration]).getBeanOption[Repo[Option]], None)
  }

  test("reports the full type of a missing bean") {
    val error = intercept[NoSuchBeanException](contextOf(classOf[OptionRepo]).getBean[Repo[List]])
    assertEquals(error.getMessage, s"No bean of type ${classOf[Repo[?]].getName}[scala.collection.immutable.List]")
  }

  test("matches a ready instance registered with its full type") {
    val singleton = RegisteredBean.of[Repo[Vector]]("vectorRepo", GenericRepo[Vector]("ready"))
    val context   = ApplicationContextBuilder.default.build(Definitions.of(classOf[OptionRepo], classOf[ListRepo]), List(singleton))
    assertEquals(context.getBean[Repo[Vector]].name, "ready")
  }

  test("does not match a ready instance registered with its class only") {
    val singleton = RegisteredBean.ofClass("vectorRepo", classOf[GenericRepo[?]], GenericRepo[Vector]("ready"))
    val context   = ApplicationContextBuilder.default.build(Nil, List(singleton))
    assertEquals(context.getBeanOption[Repo[Vector]], None)
  }

  test("rejects a dependency with an unbound type variable") {
    val error = intercept[UnsupportedDependencyTypeException](Definitions.of(classOf[UnboundRepoUser[?]]))
    assert(error.getMessage.contains("with the unbound type variable F"))
  }

  test("rejects a dependency whose type argument looks like a type lambda") {
    val error = intercept[UnsupportedDependencyTypeException](Definitions.of(classOf[LambdaRepoUser]))
    assert(error.getMessage.contains("argument scala.util.Either takes 2 type parameters"))
  }
}
