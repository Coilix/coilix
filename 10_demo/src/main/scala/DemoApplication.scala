package io.github.coilix.demo

import io.github.coilix.annotation.CoilixApplication
import io.github.coilix.boot.Coilix

@CoilixApplication
object DemoApplication {
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[DemoApplication.type](args)
  }
}
