package org.scaling.boot

trait ArgumentsParser {
  def parse(args: List[String]): ApplicationArguments
}
