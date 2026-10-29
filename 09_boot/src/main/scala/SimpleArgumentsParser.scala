package io.github.coilix.boot

final class SimpleArgumentsParser extends ArgumentsParser {

  private val optionPrefix = "--"

  override def parse(args: List[String]): ApplicationArguments =
    ApplicationArguments(args, options(args.filter(isOption)), args.filterNot(isOption))

  private def isOption(arg: String): Boolean =
    arg.startsWith(optionPrefix) && arg.length > optionPrefix.length

  private def options(optionArgs: List[String]): Map[String, List[String]] =
    optionArgs.map(toOption).groupMap(_._1)(_._2).map((name, values) => name -> values.flatten)

  private def toOption(arg: String): (String, Option[String]) =
    split(arg.drop(optionPrefix.length))

  private def split(body: String): (String, Option[String]) =
    body.indexOf('=') match {
      case -1    => (body, None)
      case index => (body.take(index), Some(body.drop(index + 1)))
    }
}
