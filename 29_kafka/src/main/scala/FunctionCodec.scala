package dev.coilix.kafka

/**
 * A [[dev.coilix.kafka.TopicCodec]] made of two functions, created by [[dev.coilix.kafka.TopicCodec.instance]].
 *
 * @param encoder
 *   the bytes of a value
 * @param decoder
 *   the value of bytes, or why they are not one
 */
case class FunctionCodec[A](encoder: A => Array[Byte], decoder: Array[Byte] => Either[Throwable, A]) extends TopicCodec[A] {

  override def encode(value: A): Array[Byte] = encoder(value)

  override def decode(bytes: Array[Byte]): Either[Throwable, A] = decoder(bytes)
}
