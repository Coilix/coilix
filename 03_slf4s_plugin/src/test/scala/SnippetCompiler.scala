package io.github.coilix.slf4s

import dotty.tools.dotc.Main
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

final class SnippetCompiler {

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

  private def classpath: String =
    List(classOf[Option[?]], classOf[CanEqual[?, ?]], classOf[org.slf4j.Logger])
      .map(locationOf)
      .distinct
      .mkString(File.pathSeparator)

  private def writeSource(source: String): Path =
    Files.writeString(Files.createTempDirectory("slf4s-src").resolve("Snippet.scala"), source)

  private def locationOf(cls: Class[?]): Path =
    Paths.get(cls.getProtectionDomain.getCodeSource.getLocation.toURI)
}
