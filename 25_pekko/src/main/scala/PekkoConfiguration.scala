package dev.coilix.pekko

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.config.Environment
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer

/**
 * The beans of the pekko module: `pekkoSettings`, `coilixActorSystem`, which creates the `ActorSystem` and stops it with the
 * context, `actorSystem` and `materializer`.
 *
 * Beans that need Pekko receive the `ActorSystem` or the `Materializer` like any other dependency, also as `using` parameters:
 *
 * ```scala
 * @Service
 * final class PriceFeed(prices: PriceRepository)(using Materializer) {
 *   def latest(count: Int): Future[Seq[Price]] = Source(prices.all).take(count).runWith(Sink.seq)
 * }
 * ```
 *
 * An application bean of type `ActorSystem` or `Materializer` takes the place of the one from this module.
 */
@Configuration
final class PekkoConfiguration {

  /** The settings under `coilix.pekko`, or the defaults in a context without an environment. */
  @Bean
  def pekkoSettings(environment: Option[Environment]): PekkoSettings =
    environment.fold(PekkoSettings())(PekkoSettings.from)

  /** Creates the `ActorSystem` on the executors of Coilix. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[ActorSystem]))
  def coilixActorSystem(
    settings:    PekkoSettings,
    compute:     TaskExecutor,
    blocking:    BlockingExecutor,
    fatalErrors: FatalErrorHandler
  ): CoilixActorSystem =
    CoilixActorSystem(settings, compute, blocking, fatalErrors)

  /** The `ActorSystem` of the application. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[ActorSystem]))
  def actorSystem(owner: CoilixActorSystem): ActorSystem =
    owner.system

  /** The materializer of Pekko Streams, which stops with the `ActorSystem`. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[Materializer]))
  def materializer(system: ActorSystem): Materializer =
    Materializer.matFromSystem(using system)
}
