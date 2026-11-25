package dev.coilix.processing

import dev.coilix.common.CoilixException

/**
 * Ends the worker of a shard when a processor fails on an element; the failure of the processor is the cause.
 *
 * @param shard
 *   the shard that processed the element, such as `invoice-mailer#2`
 * @param id
 *   the identifier of the element
 * @param delivery
 *   which delivery of the element failed
 */
final class ElementFailedException(shard: Shard, id: InputId, delivery: Int, cause: Throwable)
    extends CoilixException(s"Processing element $id (delivery $delivery) on shard $shard failed: $cause") {

  initCause(cause)
}
