package dev.coilix.pekko

import com.typesafe.config.Config
import dev.coilix.config.ConfigurationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import org.apache.pekko.dispatch.DispatcherPrerequisites
import org.apache.pekko.dispatch.ExecutorServiceConfigurator
import org.apache.pekko.dispatch.ExecutorServiceFactory

/**
 * Runs a dispatcher of Pekko on the [[dev.coilix.concurrency.BlockingExecutor]] of Coilix, by default the virtual threads.
 *
 * [[dev.coilix.pekko.CoilixActorSystem]] names this class as the `executor` of `pekko.actor.default-blocking-io-dispatcher`, so
 * blocking stages of Pekko Streams, such as file sources, wait on the same executor as blocking code of Coilix. Pekko creates it
 * by class name and it finds the executor in the [[dev.coilix.pekko.CoilixExecutorsSetup]] of the `ActorSystem`. A dispatcher of
 * the application can use it too, with `executor = "dev.coilix.pekko.BlockingExecutorConfigurator"`.
 *
 * @param config
 *   the configuration of the dispatcher
 * @param prerequisites
 *   what Pekko gives each dispatcher, including the setup of the `ActorSystem`
 */
final class BlockingExecutorConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
    extends ExecutorServiceConfigurator(config, prerequisites) {

  private val setup = prerequisites.settings.setup
    .get[CoilixExecutorsSetup]
    .getOrElse(
      throw ConfigurationException(
        "BlockingExecutorConfigurator runs only in an ActorSystem created by Coilix, which carries its blocking executor"
      )
    )

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory =
    new ExecutorServiceFactory {
      override def createExecutorService: ExecutorService = SharedExecutorService(task => setup.blocking.execute(task.run()))
    }
}
