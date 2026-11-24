package dev.coilix.boot

import dev.coilix.boot.adapted.BetweenScript
import dev.coilix.boot.adapted.ScriptRunnerAdapter
import dev.coilix.boot.adapted.UnorderedScript
import dev.coilix.boot.application.FirstRunner
import dev.coilix.boot.application.RunJournal
import dev.coilix.boot.application.SecondRunner
import dev.coilix.boot.application.UnorderedRunner
import dev.coilix.common.GenericType
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

  test("runs the runners of adapters in one list with the other runners") {
    val journal  = RunJournal()
    val registry = ImmutableBeanRegistry
      .empty(PrimaryCandidateSelector())
      .register(bean("unorderedScript", UnorderedScript(journal)))
      .register(bean("unorderedRunner", UnorderedRunner(journal)))
      .register(bean("secondRunner", SecondRunner(journal)))
      .register(bean("betweenScript", BetweenScript(journal)))
      .register(bean("firstRunner", FirstRunner(journal)))
      .register(bean("scriptRunnerAdapter", ScriptRunnerAdapter()))
    val context  = DefaultApplicationContext(registry)
    OrderedRunnerInvoker().invoke(context, SimpleArgumentsParser().parse(List("--name=Ann", "nightly")))
    assertEquals(
      journal.recorded,
      List("first", "second", "between:nightly", "unordered script:nightly", "unordered:Ann")
    )
  }

  test("does nothing without runners") {
    OrderedRunnerInvoker().invoke(
      DefaultApplicationContext(ImmutableBeanRegistry.empty(PrimaryCandidateSelector())),
      ApplicationArguments.empty
    )
  }

  private def bean(name: String, instance: AnyRef): RegisteredBean =
    RegisteredBean(name, GenericType.of(instance.getClass), isPrimary = false, instance)
}
