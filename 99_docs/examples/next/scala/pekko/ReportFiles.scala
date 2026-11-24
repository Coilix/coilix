package com.example
package pekko

import dev.coilix.annotation.Service
import java.nio.file.Path
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.FileIO
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import scala.concurrent.Future

@Service
class ReportFiles(using Materializer) {

  def write(file: Path, lines: List[String]): Future[IOResult] =
    Source(lines).map(line => ByteString(s"$line\n")).runWith(FileIO.toPath(file))
}
