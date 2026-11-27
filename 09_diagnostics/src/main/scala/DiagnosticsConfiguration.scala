package dev.coilix.diagnostics

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.config.Environment
import dev.coilix.events.EventPublisher
import java.time.Clock

/**
 * The beans of the diagnostics module: `diagnosticsSettings`, `health`, the `threads` and `startup` health indicators, the built-in
 * [[dev.coilix.diagnostics.ThreadCheck]]s, `findingReporter` and `diagnosticsMonitor`.
 */
@Configuration
final class DiagnosticsConfiguration {

  /** The settings under `coilix.diagnostics`, or the defaults in a context without an environment. */
  @Bean
  def diagnosticsSettings(environment: Option[Environment]): DiagnosticsSettings =
    environment.fold(DiagnosticsSettings())(DiagnosticsSettings.from)

  /** The health of the application, combined from every [[dev.coilix.diagnostics.HealthIndicator]] bean. */
  @Bean
  def health(indicators: List[HealthIndicator], publisher: EventPublisher): DefaultHealth =
    DefaultHealth(indicators, publisher)

  /** The `threads` liveness indicator, which goes `Down` after a deadlock when `on-deadlock` is `report`. */
  @Bean
  def threadsHealthIndicator(): SettableHealthIndicator = SettableHealthIndicator("threads", HealthKind.Liveness)

  /** The `startup` readiness indicator, `Down` until the runners of the application have finished. */
  @Bean
  def startupHealthIndicator(): StartupHealthIndicator = StartupHealthIndicator()

  /** Moves the `startup` indicator when the start ends. */
  @Bean
  def startupHealthListener(indicator: StartupHealthIndicator, health: Health): StartupHealthListener =
    StartupHealthListener(indicator, health)

  /** Finds deadlocked platform threads. */
  @Bean
  def deadlockCheck(): DeadlockCheck = DeadlockCheck()

  /** Warns about too many platform threads. */
  @Bean
  def threadCountCheck(settings: DiagnosticsSettings): ThreadCountCheck =
    ThreadCountCheck(settings.threads.warningCount, settings.threads.criticalCount)

  /** Warns about threads blocked on a monitor for long. */
  @Bean
  def longBlockedCheck(settings: DiagnosticsSettings): LongBlockedCheck = LongBlockedCheck(settings.threads.blockedAfter)

  /** Warns about a fast growth of the thread count. */
  @Bean
  def threadGrowthCheck(settings: DiagnosticsSettings): ThreadGrowthCheck =
    ThreadGrowthCheck(settings.threads.growthLimit, settings.threads.startupGrace)

  /** Logs findings, unless the application has its own [[dev.coilix.diagnostics.FindingReporter]]. */
  @Bean
  @ConditionalOnMissingBean(Array(classOf[FindingReporter]))
  def findingReporter(settings: DiagnosticsSettings, clock: Option[Clock]): LoggingFindingReporter =
    LoggingFindingReporter(settings.threads.repeatAfter, 10, clock.getOrElse(Clock.systemUTC))

  /** Runs the checks and refreshes the health on the `coilix-diagnostics` thread. */
  @Bean
  def diagnosticsMonitor(
    settings:    DiagnosticsSettings,
    checks:      List[ThreadCheck],
    reporter:    FindingReporter,
    indicator:   SettableHealthIndicator,
    health:      Health,
    fatalErrors: FatalErrorHandler,
    clock:       Option[Clock]
  ): DiagnosticsMonitor =
    DiagnosticsMonitor(
      settings,
      ThreadSampler(settings.threads.stackDepth),
      checks,
      reporter,
      indicator,
      health,
      fatalErrors,
      clock.getOrElse(Clock.systemUTC)
    )
}
