package com.example

import dev.coilix.annotation.CoilixApplication
import dev.coilix.boot.Coilix

@CoilixApplication
object FirstApplication {

  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[FirstApplication.type](args)
  }
}
