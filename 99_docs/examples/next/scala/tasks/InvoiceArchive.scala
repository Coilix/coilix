package com.example
package tasks

import dev.coilix.annotation.Service
import dev.coilix.concurrency.BlockingExecutor
import java.nio.file.Files
import java.nio.file.Path
import scala.concurrent.Future

@Service
final class InvoiceArchive(blocking: BlockingExecutor) {
  private val directory = Files.createTempDirectory("invoices")

  def store(invoice: String, content: String): Future[Path] =
    blocking.submit(Files.writeString(directory.resolve(s"$invoice.txt"), content))
}
