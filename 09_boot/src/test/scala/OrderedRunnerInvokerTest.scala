package io.github.coilix.boot

import io.github.coilix.boot.application.FirstRunner
import io.github.coilix.boot.application.RunJournal
import io.github.coilix.boot.application.SecondRunner
import io.github.coilix.boot.application.UnorderedRunner
import io.github.coilix.context.DefaultApplicationContext
import io.github.coilix.context.ImmutableBeanRegistry
import io.github.coilix.context.PrimaryCandidateSelector
import io.github.coilix.context.RegisteredBean
import munit.FunSuite

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
