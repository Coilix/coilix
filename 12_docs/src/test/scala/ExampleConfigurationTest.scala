package com.example

import dev.coilix.config.ConfigFormat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*
import scala.util.Using

class ExampleConfigurationTest extends munit.FunSuite {

  private val resources: Path = Paths.get(sys.props.getOrElse("coilix.docs.resources", "12_docs/examples/next/resources"))

  test("the examples have configuration files") {
    assert(configurationFiles.nonEmpty, s"No configuration files in $resources")
  }

  configurationFiles.foreach(file => test(s"${resources.relativize(file)} is a valid configuration file")(assertParses(file)))

  private def configurationFiles: List[Path] =
    Using.resource(Files.walk(resources))(_.iterator().asScala.toList.filter(file => formatOf(file).isDefined).sorted)

  private def formatOf(file: Path): Option[ConfigFormat] =
    ConfigFormat.byPrecedence.find(_.extensions.contains(extensionOf(file)))

  private def extensionOf(file: Path): String =
    file.getFileName.toString.split('.').lastOption.getOrElse("")

  private def assertParses(file: Path): Unit =
    formatOf(file).foreach(format => assert(!format.parse(file.toUri.toURL).isEmpty, s"$file is empty"))
}
