package dev.coilix.boot
package jobs

import dev.coilix.context.CoilixModule
import java.beans.JavaBean
import java.lang.annotation.Annotation

final class JobsModule extends CoilixModule {
  override def name: String = "jobs"

  override def components: List[Class[?]] = List(classOf[JobRunner], classOf[ModuleJob])

  override def collects: List[Class[? <: Annotation]] = List(classOf[JavaBean])
}
