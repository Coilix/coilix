package dev.coilix.pekko

import org.apache.pekko.actor.CoordinatedShutdown

/** The reason of the coordinated shutdown that [[dev.coilix.pekko.CoilixActorSystem]] runs when the context closes. */
case object CoilixShutdownReason extends CoordinatedShutdown.Reason
