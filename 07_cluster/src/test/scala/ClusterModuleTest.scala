package dev.coilix.cluster

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import dev.coilix.context.ServiceLoaderModuleLoader
import munit.FunSuite

final class ClusterModuleTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("is found on the classpath") {
    assert(ServiceLoaderModuleLoader(getClass.getClassLoader, Environment.empty).load().exists(_.name == "cluster"))
  }

  test("a context without settings runs as the only replica") {
    val context = contextOf(Nil)
    try {
      assertEquals(context.getBean[ReplicaIdentity].replicaCount, 1)
    } finally {
      context.close()
    }
  }

  test("the identity comes from the configuration") {
    val environment = Environment.of(
      TypesafeConfigTree(ConfigFactory.parseString("coilix.cluster { identity = static, replicas = 5, replica-index = 4 }"))
    )
    val context     = contextOf(List(RegisteredBean.of[Environment]("environment", environment)))
    try {
      val identity = context.getBean[ReplicaIdentity]
      assertEquals((identity.self.index, identity.replicaCount), (4, 5))
    } finally {
      context.close()
    }
  }

  private def contextOf(singletons: List[RegisteredBean]): ConfigurableApplicationContext =
    ApplicationContextBuilder.default.build(ClusterModule().components.flatMap(BeanDefinitionFactory.default.create), singletons)
}
