package dev.coilix.concurrency

import munit.FunSuite

final class ThreadFactoriesTest extends FunSuite {
  private val virtualThreadsExpected = Runtime.version.feature >= 21

  test("virtual threads are available from JDK 21") {
    assertEquals(ThreadFactories.virtualThreadsAvailable, virtualThreadsExpected)
  }

  test("names platform threads with a prefix and a number") {
    val factory = ThreadFactories.named("worker-", virtual = false)
    val threads = List(factory.newThread(() => ()), factory.newThread(() => ()))
    assertEquals(
      threads.map(thread => (thread.getName, thread.isDaemon, ThreadFactories.isVirtual(thread))),
      List(
        ("worker-1", true, false),
        ("worker-2", true, false)
      )
    )
  }

  test("creates virtual threads when the JDK has them") {
    val thread = ThreadFactories.named("light-", virtual = true).newThread(() => ())
    assertEquals((thread.getName, ThreadFactories.isVirtual(thread)), ("light-1", virtualThreadsExpected))
  }
}
