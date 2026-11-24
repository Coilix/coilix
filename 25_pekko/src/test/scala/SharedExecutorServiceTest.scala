package dev.coilix.pekko

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite

final class SharedExecutorServiceTest extends FunSuite {

  test("runs tasks on the given executor until it is shut down") {
    val ran     = AtomicReference(List.empty[String])
    val service = SharedExecutorService(task => task.run())
    service.execute(() => { val _ = ran.updateAndGet(_ :+ "first") })
    service.shutdown()
    intercept[RejectedExecutionException](service.execute(() => ()))
    assertEquals(
      (ran.get, service.isShutdown, service.isTerminated, service.awaitTermination(1, TimeUnit.SECONDS)),
      (List("first"), true, true, true)
    )
  }

  test("shutdownNow returns no pending tasks, because the executor behind it keeps them") {
    val service = SharedExecutorService(task => task.run())
    assertEquals((service.shutdownNow().isEmpty, service.isShutdown), (true, true))
  }
}
