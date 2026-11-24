package dev.coilix.pekko

import dev.coilix.common.CoilixException

/**
 * Reported to the [[dev.coilix.concurrency.FatalErrorHandler]] when the `ActorSystem` of the application stops without the
 * context asking for it, as Pekko does after a fatal error in an actor or a stream.
 *
 * @param systemName
 *   the name of the stopped `ActorSystem`
 */
final class ActorSystemStoppedException(systemName: String)
    extends CoilixException(s"The ActorSystem '$systemName' stopped while the context was running, usually after a fatal error")
