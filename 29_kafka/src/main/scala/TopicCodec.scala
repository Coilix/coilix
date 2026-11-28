package dev.coilix.kafka

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.util.Try

/**
 * Turns keys or values of a topic into bytes and back.
 *
 * Instances for `String`, `Array[Byte]`, `Int`, `Long` and `UUID` are given; numbers use the format of the serializers of the
 * Kafka client, so other Kafka applications read them. Another type gets its codec with
 * [[dev.coilix.kafka.TopicCodec.instance]] or by mapping a given one:
 *
 * ```scala
 * given TopicCodec[OrderId] = TopicCodec[String].imap(OrderId(_))(_.value)
 * ```
 *
 * A record without a key or value (a tombstone) is decoded from an empty array.
 *
 * @tparam A
 *   the type of the keys or values
 */
trait TopicCodec[A] {

  /** The bytes of `value`. */
  def encode(value: A): Array[Byte]

  /** The value of `bytes`, or why they are not one. */
  def decode(bytes: Array[Byte]): Either[Throwable, A]

  /** A codec of `B` that encodes and decodes through this one. */
  final def imap[B](to: A => B)(from: B => A): TopicCodec[B] =
    TopicCodec.instance[B](value => encode(from(value)), bytes => decode(bytes).flatMap(a => Try(to(a)).toEither))
}

/** The given codecs and the creation of new ones. */
object TopicCodec {

  /** The given codec of `A`. */
  def apply[A](using codec: TopicCodec[A]): TopicCodec[A] = codec

  /** A codec from two functions. */
  def instance[A](encoder: A => Array[Byte], decoder: Array[Byte] => Either[Throwable, A]): TopicCodec[A] =
    FunctionCodec(encoder, decoder)

  /** Text in UTF-8. */
  given string: TopicCodec[String] =
    instance(_.getBytes(StandardCharsets.UTF_8), bytes => Right(String(bytes, StandardCharsets.UTF_8)))

  /** The bytes as they are. */
  given bytes: TopicCodec[Array[Byte]] = instance(identity, Right(_))

  /** Four bytes, most significant first. */
  given int: TopicCodec[Int] =
    instance(value => ByteBuffer.allocate(Integer.BYTES).putInt(value).array(), sized(Integer.BYTES)(_.getInt))

  /** Eight bytes, most significant first. */
  given long: TopicCodec[Long] =
    instance(value => ByteBuffer.allocate(java.lang.Long.BYTES).putLong(value).array(), sized(java.lang.Long.BYTES)(_.getLong))

  /** The text form of the identifier in UTF-8. */
  given uuid: TopicCodec[UUID] = string.imap(UUID.fromString)(_.toString)

  private def sized[A](size: Int)(read: ByteBuffer => A)(bytes: Array[Byte]): Either[Throwable, A] =
    if (bytes.length == size) {
      Right(read(ByteBuffer.wrap(bytes)))
    } else {
      Left(IllegalArgumentException(s"expected $size bytes, got ${bytes.length}"))
    }
}
