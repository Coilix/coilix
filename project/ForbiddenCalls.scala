import sbt.*

object ForbiddenCalls {
  private val forbidden = """\b(println|print|printf)\s*\(""".r

  def check(sources: Seq[File]): Unit =
    violations(sources) match {
      case Nil   => ()
      case found => sys.error(s"Forbidden console output, use @Slf4s logging instead:\n${found.mkString("\n")}")
    }

  private def violations(sources: Seq[File]): List[String] =
    sources.toList.filter(_.getName.endsWith(".scala")).flatMap(violationsIn)

  private def violationsIn(source: File): List[String] =
    IO.readLines(source).zipWithIndex.collect {
      case (line, index) if forbidden.findFirstIn(line).isDefined => s"${source.getPath}:${index + 1}: ${line.trim}"
    }
}
