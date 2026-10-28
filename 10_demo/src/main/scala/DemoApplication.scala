package org.scaling.demo

import org.scaling.annotation.ScalingBootApplication
import org.scaling.boot.Scaling

@ScalingBootApplication
object DemoApplication {
  def main(args: Array[String]): Unit = {
    val _ = Scaling.run[DemoApplication.type](args)
  }
}
