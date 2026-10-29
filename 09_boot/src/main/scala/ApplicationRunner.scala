package io.github.coilix.boot

trait ApplicationRunner {
  def run(arguments: ApplicationArguments): Unit
}
