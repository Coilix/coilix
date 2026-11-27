package dev.coilix.logging

import cats.effect.IO
import cats.syntax.all.*
import munit.CatsEffectSuite
import org.slf4j.MDC
import scala.jdk.CollectionConverters.*

final class FiberMdcAdapterTest extends CatsEffectSuite {

  private def job: IO[Option[String]] = IO(Option(MDC.get("job")))

  test("the MDC of the logging module follows the fiber") {
    assert(MDC.getMDCAdapter.isInstanceOf[FiberMdcAdapter])
  }

  test("keeps the MDC of a fiber across thread changes") {
    for {
      _       <- IO(MDC.put("job", "export"))
      _       <- IO.cede
      blocked <- IO.blocking(Option(MDC.get("job")))
      later   <- job
      _       <- IO(MDC.remove("job"))
      removed <- job
    } yield assertEquals((blocked, later, removed), (Some("export"), Some("export"), None))
  }

  test("a started fiber begins with a copy of the MDC of its parent") {
    for {
      _      <- IO(MDC.put("job", "import"))
      child  <- (job <* IO(MDC.put("job", "changed in the child"))).start.flatMap(_.joinWithNever)
      parent <- job
      _      <- IO(MDC.clear())
    } yield assertEquals((child, parent), (Some("import"), Some("import")))
  }

  test("fibers that were not started by each other do not share the MDC") {
    for {
      first  <- (IO(MDC.put("job", "first")) *> IO.sleep(scala.concurrent.duration.DurationInt(20).millis) *> job).start
      second <- (IO.sleep(scala.concurrent.duration.DurationInt(5).millis) *> job).start
      seen   <- (first.joinWithNever, second.joinWithNever).tupled
    } yield assertEquals(seen, (Some("first"), None))
  }

  test("copies and replaces the whole MDC of a fiber") {
    for {
      _      <- IO(MDC.setContextMap(Map("job" -> "audit", "tenant" -> "north").asJava))
      copied <- IO(MDC.getCopyOfContextMap.asScala.toMap)
      _      <- IO(MDC.setContextMap(java.util.HashMap[String, String]()))
      empty  <- IO(MDC.getCopyOfContextMap.asScala.toMap)
    } yield assertEquals((copied, empty), (Map("job" -> "audit", "tenant" -> "north"), Map.empty[String, String]))
  }

  test("a thread without a fiber keeps its own MDC") {
    IO.blocking {
      val seen = java.util.concurrent.atomic.AtomicReference(Option.empty[String])
      val other = Thread(() => {
        MDC.put("job", "thread")
        seen.set(Option(MDC.get("job")))
        MDC.clear()
      })
      other.start()
      other.join()
      seen.get
    }.map(seen => assertEquals(seen, Some("thread")))
  }
}
