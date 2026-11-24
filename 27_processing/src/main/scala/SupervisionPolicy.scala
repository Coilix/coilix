package dev.coilix.processing

/**
 * The policies that supervise the shards of one processor.
 *
 * @param restart
 *   what follows a failure
 * @param completion
 *   what follows the end of the work
 */
final case class SupervisionPolicy(restart: RestartPolicy = RestartPolicy(), completion: CompletionPolicy = CompletionPolicy.Stop)
