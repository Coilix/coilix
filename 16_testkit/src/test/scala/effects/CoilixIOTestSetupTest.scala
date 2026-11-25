package dev.coilix.testkit
package effects

import cats.effect.IO
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.effects.repo.Repo
import munit.CatsEffectSuite

final class CoilixIOTestSetupTest extends CatsEffectSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forPackages("dev.coilix.testkit.effects.repo")

  test("useIO gives the context to the body and closes it afterwards") {
    setup
      .useIO(bean[Repo[IO]].find(1).map(found => (found, summon[ConfigurableApplicationContext])))
      .map((found, context) => assertEquals((found, context.isActive), (Some("tea"), false)))
  }

  test("useIO closes the context when the body fails") {
    val opened = IO.ref(Option.empty[ConfigurableApplicationContext])
    opened.flatMap { reference =>
      setup
        .useIO(reference.set(Some(summon[ConfigurableApplicationContext])) *> IO.raiseError(IllegalStateException("broken")))
        .attempt
        .flatMap(result => reference.get.map(context => (result.left.map(_.getMessage), context.map(_.isActive))))
        .map(assertEquals(_, (Left("broken"), Some(false))))
    }
  }

  test("resource gives the context to use in IO") {
    setup.resource.use(context => IO(context.getBeansOfType[Repo[Option]].size)).map(assertEquals(_, 1))
  }
}
