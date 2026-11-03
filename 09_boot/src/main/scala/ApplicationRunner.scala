package dev.coilix.boot

trait ApplicationRunner {
  def run(arguments: ApplicationArguments): Unit
}
