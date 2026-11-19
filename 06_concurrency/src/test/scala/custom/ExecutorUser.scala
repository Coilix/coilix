package dev.coilix.concurrency
package custom

import dev.coilix.annotation.Component

@Component
final class ExecutorUser(val executor: TaskExecutor, val scheduler: TaskScheduler)
