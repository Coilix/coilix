package dev.coilix.context
package modules

final class SampleModule extends CoilixModule {
  override def name: String = "sample"

  override def components: List[Class[?]] = List(classOf[SampleService])
}
