package com.example

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Component
@Slf4s
final class ReportScheduler(@Value("${app.report-threads:2}") threads: Int) {
  private val executor: ExecutorService = Executors.newFixedThreadPool(threads)

  def submit(report: Runnable): Unit = executor.execute(report)

  @PostConstruct
  private def checkThreads(): Unit = {
    require(threads > 0, s"app.report-threads must be positive, but is $threads")
    log.info(s"Report scheduler is ready with $threads threads")
  }

  @PreDestroy
  private def stop(): Unit = {
    executor.shutdown()
    log.info("Report scheduler stopped")
  }
}
