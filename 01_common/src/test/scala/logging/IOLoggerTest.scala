package dev.coilix.common
package logging

import cats.data.EitherT
import cats.effect.IO
import cats.effect.Resource
import cats.effect.Sync
import java.util.concurrent.atomic.AtomicInteger
import munit.CatsEffectSuite
import org.slf4j.event.Level

final class IOLoggerTest extends CatsEffectSuite {

  private def allLevels: Set[Level] = Level.values.toSet

  test("logs nothing until the IO runs") {
    val recording = RecordingLogger(allLevels)
    val logged    = IOLogger(recording).info("ready")
    IO(assertEquals(recording.recorded, Vector.empty)) *> logged *> IO(assertEquals(recording.recorded, Vector("INFO ready")))
  }

  test("logs at every level, with an error when given") {
    val recording = RecordingLogger(allLevels)
    val logger    = IOLogger(recording)
    val failure   = IllegalStateException("broken")
    for {
      _ <- logger.trace("t")
      _ <- logger.debug("d")
      _ <- logger.info("i")
      _ <- logger.warn("w", failure)
      _ <- logger.error("e", failure)
      _ <- logger.trace("t", failure)
      _ <- logger.debug("d", failure)
      _ <- logger.info("i", failure)
      _ <- logger.warn("w")
      _ <- logger.error("e")
    } yield assertEquals(
      recording.recorded,
      Vector(
        "TRACE t",
        "DEBUG d",
        "INFO i",
        "WARN w (broken)",
        "ERROR e (broken)",
        "TRACE t (broken)",
        "DEBUG d (broken)",
        "INFO i (broken)",
        "WARN w",
        "ERROR e"
      )
    )
  }

  test("does not build the message of a disabled level") {
    val built     = AtomicInteger()
    val recording = RecordingLogger(Set(Level.INFO, Level.WARN, Level.ERROR))
    val logger    = IOLogger(recording)
    def message: String = s"expensive ${built.incrementAndGet()}"
    for {
      _ <- logger.debug(message)
      _ <- logger.trace(message, IllegalStateException("ignored"))
      _ <- logger.info(message)
    } yield assertEquals((built.get, recording.recorded), (1, Vector("INFO expensive 1")))
  }

  test("logs in any effect with Sync when the effect is given, also inside EitherT and Resource") {
    val recording  = RecordingLogger(allLevels)
    val logger     = IOLogger(recording)
    val inEither   = for {
      _ <- logger[[A] =>> EitherT[IO, String, A]].info("in EitherT")
      _ <- EitherT.leftT[IO, Unit]("stopped")
      _ <- logger[[A] =>> EitherT[IO, String, A]].info("never logged")
    } yield ()
    val inResource = logger[[A] =>> Resource[IO, A]].debug("in Resource", IllegalStateException("noted"))
    for {
      stopped <- inEither.value
      _       <- inResource.use_
      _       <- polymorphic[IO](logger)
    } yield {
      assertEquals(stopped, Left("stopped"))
      assertEquals(recording.recorded, Vector("INFO in EitherT", "DEBUG in Resource (noted)", "WARN polymorphic"))
    }
  }

  private def polymorphic[F[_]: Sync](logger: IOLogger): F[Unit] = logger[F].warn("polymorphic")
}
