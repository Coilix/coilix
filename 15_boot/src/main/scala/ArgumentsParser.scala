package dev.coilix.boot

/** Splits command-line arguments into options and other arguments. */
trait ArgumentsParser {

  /** The parsed form of `args`. */
  def parse(args: List[String]): ApplicationArguments
}
