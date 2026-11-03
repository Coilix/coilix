package dev.coilix.boot

import dev.coilix.boot.application.FirstRunner
import dev.coilix.boot.application.RunJournal
import dev.coilix.boot.application.SecondRunner
import dev.coilix.boot.application.UnorderedRunner
import dev.coilix.context.DefaultApplicationContext
import dev.coilix.context.ImmutableBeanRegistry
import dev.coilix.context.PrimaryCandidateSelector
import dev.coilix.context.RegisteredBean
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
