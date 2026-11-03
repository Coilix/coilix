package dev.coilix.boot

/**
 * Treats every argument that starts with `--` and has a name as an option: `--name=value` gives a value, `--name` gives an
 * option without a value, and a repeated option collects all its values. Every other argument, including a lone `--`, is
 * a non-option argument.
 *
 * This is the parser used by [[dev.coilix.boot.CoilixApplicationRunner.default]].
 */
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
