package dev.coilix.pekko

import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.pekko.feed.PriceFeed
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.util.concurrent.TimeUnit
import munit.FunSuite
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

final class PekkoModuleTest extends FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forPackages("dev.coilix.pekko.feed")

  test("injects the materializer as a using parameter and runs streams on the task executor") {
    setup.use {
      assert(Await.result(bean[PriceFeed].threadOfLatest, 5.seconds).startsWith("coilix-compute-"))
    }
  }

  test("runs the blocking dispatcher on the blocking executor") {
    setup.use {
      val dispatcher = bean[ActorSystem].dispatchers.lookup("pekko.actor.default-blocking-io-dispatcher")
      assert(Await.result(Future(Thread.currentThread.getName)(using dispatcher), 5.seconds).startsWith("coilix-blocking-"))
    }
  }

  test("uses the default dispatcher as the internal dispatcher") {
    setup.use {
      val system = bean[ActorSystem]
      assertEquals(system.dispatchers.lookup("pekko.actor.internal-dispatcher").id, system.dispatchers.defaultGlobalDispatcher.id)
    }
  }

  test("takes the name and the configuration of Pekko from coilix.pekko") {
    setup
      .withProperties(
        "coilix.pekko.system-name"                                                -> "shop",
        "coilix.pekko.config.pekko.stream.materializer.max-input-buffer-size"     -> "32",
        "coilix.pekko.config.pekko.coordinated-shutdown.run-by-jvm-shutdown-hook" -> "on"
      )
      .use {
        val config = bean[ActorSystem].settings.config
        assertEquals(
          (
            bean[ActorSystem].name,
            config.getInt("pekko.stream.materializer.max-input-buffer-size"),
            config.getBoolean("pekko.coordinated-shutdown.run-by-jvm-shutdown-hook")
          ),
          ("shop", 32, false)
        )
      }
  }

  test("stops the ActorSystem when the context closes, without reporting it as a fatal error") {
    val handler = RecordingFatalErrorHandler()
    val system  = setup.replaceBean[FatalErrorHandler](handler).use((bean[ActorSystem], bean[Materializer])._1)
    assertEquals((system.whenTerminated.isCompleted, handler.failures), (true, Nil))
  }

  test("reports an ActorSystem that stops while the context runs to the fatal error handler") {
    val handler = RecordingFatalErrorHandler()
    setup.replaceBean[FatalErrorHandler](handler).use {
      val _ = bean[ActorSystem].terminate()
      assert(handler.called.await(5, TimeUnit.SECONDS))
    }
    assert(handler.failures.exists(_.isInstanceOf[ActorSystemStoppedException]), handler.failures.toString)
  }
}
