package dev.coilix.slf4s

import dotty.tools.dotc.Main
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

final class SnippetCompiler(withEffectfulLogger: Boolean = false) {

  def errors(source: String): List[String] =
    Main.process(arguments(writeSource(source)).toArray).allErrors.map(_.message)

  private def arguments(sourceFile: Path): List[String] =
    List(
      s"-Xplugin:${locationOf(classOf[Slf4sPlugin])}",
      "-Xplugin-require:slf4s",
      "-classpath",
      classpath,
      "-d",
      Files.createTempDirectory("slf4s-out").toString,
      sourceFile.toString
    )

  private def classpath: String = (List(classOf[Option[?]], classOf[CanEqual[?, ?]], classOf[org.slf4j.Logger]) ++ effectfulClasses)
    .map(locationOf)
    .distinct
    .mkString(File.pathSeparator)

  private def effectfulClasses: List[Class[?]] =
    if (withEffectfulLogger) {
      List(
        classOf[dev.coilix.common.logging.IOLogger],
        classOf[cats.effect.IO[?]],
        classOf[cats.effect.kernel.Sync[?]],
        classOf[cats.effect.std.Console[?]],
        classOf[cats.Functor[?]],
        classOf[cats.kernel.Eq[?]]
      )
    } else {
      Nil
    }

  private def writeSource(source: String): Path =
    Files.writeString(Files.createTempDirectory("slf4s-src").resolve("Snippet.scala"), source)

  private def locationOf(cls: Class[?]): Path =
    Paths.get(cls.getProtectionDomain.getCodeSource.getLocation.toURI)
}
