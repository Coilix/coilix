package io.github.coilix.boot

trait ArgumentsParser {
  def parse(args: List[String]): ApplicationArguments
}
