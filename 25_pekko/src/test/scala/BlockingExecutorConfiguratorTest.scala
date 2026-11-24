package dev.coilix.pekko

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import munit.FunSuite
import org.apache.pekko.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class BlockingExecutorConfiguratorTest extends FunSuite {

  test("refuses to run in an ActorSystem that Coilix did not create") {
    val config = ConfigFactory
      .parseString("""blocking { type = Dispatcher, executor = "dev.coilix.pekko.BlockingExecutorConfigurator" }""")
      .withFallback(ConfigFactory.defaultReference())
    val system = ActorSystem("plain", config)
    try {
      val failure = intercept[Exception](system.dispatchers.lookup("blocking"))
      assert(
        List.unfold(
          Option[Throwable](failure)
        )(_.map(cause => (cause, Option(cause.getCause)))).exists(_.isInstanceOf[ConfigurationException]),
        failure.toString
      )
    } finally {
      val _ = Await.result(system.terminate(), 10.seconds)
    }
  }
}
