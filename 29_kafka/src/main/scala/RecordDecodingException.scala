package dev.coilix.kafka

import dev.coilix.common.CoilixException

/**
 * Thrown when the [[dev.coilix.kafka.TopicCodec]] of a topic cannot decode the key or value of a record, which fails the stream
 * that reads it.
 *
 * @param topic
 *   the topic of the record
 * @param cause
 *   why the bytes could not be decoded
 */
final class RecordDecodingException(val topic: String, cause: Throwable)
    extends CoilixException(s"Cannot decode a record of topic $topic: ${cause.getMessage}") {
  initCause(cause)
}
