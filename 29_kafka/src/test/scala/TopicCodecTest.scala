package dev.coilix.kafka

import java.util.UUID
import munit.FunSuite
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.LongSerializer

class TopicCodecTest extends FunSuite {

  private final case class OrderId(value: String)

  private def roundTrip[A: TopicCodec](value: A): Either[Throwable, A] =
    TopicCodec[A].decode(TopicCodec[A].encode(value))

  test("encodes and decodes text, bytes, numbers and identifiers") {
    val id = UUID.randomUUID()
    assertEquals(roundTrip("zażółć"), Right("zażółć"))
    assertEquals(roundTrip(Array[Byte](1, 2, 3)).map(_.toList), Right(List[Byte](1, 2, 3)))
    assertEquals(roundTrip(42), Right(42))
    assertEquals(roundTrip(-7L), Right(-7L))
    assertEquals(roundTrip(id), Right(id))
  }

  test("writes numbers like the serializers of the Kafka client") {
    assertEquals(TopicCodec[Int].encode(513).toList, IntegerSerializer().serialize("t", 513).toList)
    assertEquals(TopicCodec[Long].encode(513L).toList, LongSerializer().serialize("t", 513L).toList)
  }

  test("rejects bytes of the wrong size for numbers") {
    assertEquals(TopicCodec[Long].decode(Array[Byte](1, 2)).left.map(_.getMessage), Left("expected 8 bytes, got 2"))
  }

  test("maps a codec to another type and returns a failure of the mapping") {
    given TopicCodec[OrderId] = TopicCodec[String].imap(OrderId(_))(_.value)
    val positive              = TopicCodec[Int].imap(n => if (n > 0) n else throw IllegalArgumentException("not positive"))(identity)
    assertEquals(roundTrip(OrderId("A-1")), Right(OrderId("A-1")))
    assertEquals(positive.decode(TopicCodec[Int].encode(-1)).left.map(_.getMessage), Left("not positive"))
  }
}
