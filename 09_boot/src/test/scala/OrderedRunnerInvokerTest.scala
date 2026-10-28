package org.scaling.boot

import munit.FunSuite
import org.scaling.boot.application.FirstRunner
import org.scaling.boot.application.RunJournal
import org.scaling.boot.application.SecondRunner
import org.scaling.boot.application.UnorderedRunner
import org.scaling.context.DefaultApplicationContext
import org.scaling.context.ImmutableBeanRegistry
import org.scaling.context.PrimaryCandidateSelector
import org.scaling.context.RegisteredBean

final class OrderedRunnerInvokerTest extends FunSuite {

  test("runs runners by their order with unordered ones last") {
    val journal = RunJournal()
    val context = DefaultApplicationContext(
      ImmutableBeanRegistry.empty(PrimaryCandidateSelector())
        .register(bean("unorderedRunner", UnorderedRunner(journal)))
        .register(bean("secondRunner", SecondRunner(journal)))
        .register(bean("firstRunner", FirstRunner(journal)))
    )
    OrderedRunnerInvoker().invoke(context, SimpleArgumentsParser().parse(List("--name=Ann")))
    assertEquals(journal.recorded, List("first", "second", "unordered:Ann"))
  }

  test("does nothing without runners") {
    OrderedRunnerInvoker().invoke(
      DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector())),
      ApplicationArguments.empty
    )
  }

  private def bean(name: String, instance: AnyRef): RegisteredBean =
    RegisteredBean(name, instance.getClass, isPrimary = false, instance)
}
