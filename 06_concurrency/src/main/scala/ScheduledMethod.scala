package dev.coilix.concurrency

import java.lang.reflect.Method

/**
 * A [[dev.coilix.annotation.Scheduled]] method of a bean, found by the [[dev.coilix.concurrency.ScheduledMethodRegistrar]].
 *
 * @param label
 *   the method and bean name for log and error messages
 * @param bean
 *   the instance the method is invoked on
 * @param method
 *   the accessible method without parameters
 * @param trigger
 *   when the method runs
 */
final case class ScheduledMethod(label: String, bean: AnyRef, method: Method, trigger: ScheduledTrigger)
