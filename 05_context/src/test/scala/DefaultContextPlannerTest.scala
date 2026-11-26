package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.conditions.ClockDefaults
import dev.coilix.context.conditions.FixedClock
import dev.coilix.context.effects.EffectConfiguration
import dev.coilix.context.effects.Report
import dev.coilix.context.effects.ReportReader
import dev.coilix.context.effects.TestEffects
import dev.coilix.context.wiring.DefaultGreetingService
import dev.coilix.context.wiring.EnglishGreeter
import munit.FunSuite

final class DefaultContextPlannerTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  private val planner = ContextPlanner.default

  test("provides the ready instances before creating beans") {
    val greeter = RegisteredBean.of("englishGreeter", EnglishGreeter())
    val plan    = planned(Definitions.of(classOf[DefaultGreetingService]), List(greeter))
    assertEquals(plan.steps, PlanStep.Provide(greeter) :: plan.creationOrder.map(PlanStep.Create(_)))
  }

  test("creates each bean after its dependencies") {
    val plan = planned(Definitions.of(classOf[DefaultGreetingService], classOf[EnglishGreeter]), Nil)
    assertEquals(plan.creationOrder.map(_.beanName), List("englishGreeter", "defaultGreetingService"))
  }

  test("gives each bean its dependency level") {
    val plan = planned(Definitions.of(classOf[ReportReader]) ++ TestEffects.definitions, Nil)
    assertEquals(
      plan.levels.map(_.map(_.beanName)),
      List(List("effectConfiguration"), List("archive", "report"), List("reportReader"))
    )
  }

  test("leaves out definitions whose conditions do not hold") {
    val plan = planned(Definitions.of(classOf[ClockDefaults]), List(RegisteredBean.of("clock", FixedClock())))
    assertEquals(plan.creationOrder.map(_.beanName), List("clockDefaults"))
  }

  test("resolves dependencies on the value of an effectful bean") {
    val plan = planned(Definitions.of(classOf[ReportReader]) ++ TestEffects.definitions, Nil)
    assertEquals(plan.creationOrder.map(_.beanName), List("effectConfiguration", "archive", "report", "reportReader"))
  }

  test("plans no step for an empty context") {
    assertEquals(planner.plan(Nil, Nil), Right(ContextPlan(Nil)))
  }

  test("gives the error of the resolver when a dependency is missing") {
    assertEquals(
      planner.plan(Definitions.of(classOf[DefaultGreetingService]), Nil).left.map(_.message),
      Left("No bean satisfies dependency 'dev.coilix.context.wiring.Greeter' required by 'defaultGreetingService'")
    )
  }

  test("registers an effectful bean under its value type") {
    val report = TestEffects.scanner.scan(classOf[EffectConfiguration]).filter(_.beanName == "report")
    assertEquals(report.map(_.beanType), List(GenericType.of(classOf[Report])))
  }

  private def planned(definitions: List[BeanDefinition], provided: List[RegisteredBean]): ContextPlan =
    planner.plan(definitions, provided).fold(error => fail(error.message), identity)
}
