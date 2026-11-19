package dev.coilix.context

import dev.coilix.common.GenericType
import dev.coilix.context.effects.Later
import dev.coilix.context.effects.Report
import dev.coilix.context.wiring.EnglishGreeter
import dev.coilix.context.wiring.PolishGreeter
import munit.FunSuite

final class ContextPlanTest extends FunSuite {
  private val provided = RegisteredBean.of("englishGreeter", EnglishGreeter())
  private val first    = Definitions.of(classOf[PolishGreeter]).map(_.copy(beanName = "first"))
  private val second   = Definitions.of(classOf[PolishGreeter]).map(_.copy(beanName = "second", result = deferred))
  private val plan     = ContextPlan(PlanStep.Provide(provided) :: (first ++ second).map(PlanStep.Create(_)))

  test("lists the ready instances") {
    assertEquals(plan.provided, List(provided))
  }

  test("lists the beans in creation order") {
    assertEquals(plan.creationOrder.map(_.beanName), List("first", "second"))
  }

  test("destroys the beans in reverse creation order") {
    assertEquals(plan.destructionOrder.map(_.beanName), List("second", "first"))
  }

  test("lists the beans created by effects") {
    assertEquals(plan.effectful.map(_.beanName), List("second"))
  }

  private def deferred: BeanResult = {
    val report = GenericType.of(classOf[Report])
    BeanResult.Deferred(GenericType.Concrete(classOf[Later[?]], List(report)), report)
  }
}
