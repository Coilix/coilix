package dev.coilix.pekko

import dev.coilix.concurrency.BlockingExecutor
import org.apache.pekko.actor.setup.Setup

/**
 * Hands the executors of Coilix to the dispatchers of an `ActorSystem` created by [[dev.coilix.pekko.CoilixActorSystem]], which
 * Pekko creates by class name and so cannot receive beans.
 *
 * @param blocking
 *   runs the tasks of `pekko.actor.default-blocking-io-dispatcher`
 */
final case class CoilixExecutorsSetup(blocking: BlockingExecutor) extends Setup
