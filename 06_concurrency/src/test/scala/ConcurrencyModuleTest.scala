package dev.coilix.concurrency

import dev.coilix.concurrency.custom.CallerRunsExecutor
import dev.coilix.concurrency.custom.CustomExecutorConfiguration
import dev.coilix.concurrency.custom.ExecutorUser
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.context.ServiceLoaderModuleLoader
import munit.FunSuite

final class ConcurrencyModuleTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("is found on the classpath") {
    assert(ServiceLoaderModuleLoader(getClass.getClassLoader, Environment.empty).load().exists(_.name == "concurrency"))
  }

  test("injects the executor and the scheduler") {
    val context = contextOf(classOf[ExecutorUser])
    try {
      val user = context.getBean[ExecutorUser]
      assertEquals(
        (user.executor eq context.getBean[TaskExecutor], user.scheduler eq context.getBean[TaskScheduler]),
        (true, true)
      )
    } finally {
      context.close()
    }
  }

  test("closes the executor and the scheduler with the context") {
    val context   = contextOf()
    val executor  = context.getBean[ExecutorServiceTaskExecutor]
    val blocking  = context.getBean[ExecutorServiceBlockingExecutor]
    val scheduler = context.getBean[ScheduledExecutorTaskScheduler]
    context.close()
    assertEquals((executor.isClosed, blocking.isClosed, scheduler.isClosed), (true, true, true))
  }

  test("uses the default settings in a context without an environment") {
    val context =
      ApplicationContextBuilder.default.build(ConcurrencyModule().components.flatMap(BeanDefinitionFactory.default.create))
    try {
      assertEquals(context.getBean[ConcurrencySettings], ConcurrencySettings())
    } finally {
      context.close()
    }
  }

  test("leaves the default executors out, without creating their pools, when the application provides its own") {
    val context = contextOf(classOf[CustomExecutorConfiguration], classOf[ExecutorUser])
    try {
      assertEquals(
        (context.getBean[ExecutorUser].executor.isInstanceOf[CallerRunsExecutor], context.getBeansOfType[TaskExecutor].size),
        (true, 1)
      )
    } finally {
      context.close()
    }
  }

  private def contextOf(classes: Class[?]*): ConfigurableApplicationContext = {
    val components = ConcurrencyModule().components ++ classes
    ApplicationContextBuilder.default.build(
      components.flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", Environment.empty))
    )
  }
}
