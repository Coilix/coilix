package dev.coilix.demo

import dev.coilix.annotation.CoilixApplication
import dev.coilix.boot.Coilix

@CoilixApplication
object DemoApplication {
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[DemoApplication.type](args)
  }
}
