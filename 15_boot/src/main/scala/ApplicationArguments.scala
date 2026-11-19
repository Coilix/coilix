package dev.coilix.boot

/**
 * The command-line arguments of the application, split into options and other arguments.
 *
 * Coilix registers the arguments as a bean named `applicationArguments` and passes them to every
 * [[dev.coilix.boot.ApplicationRunner]]. Options with a value, such as `--server.port=9090`, also override the configuration.
 *
 * @param sourceArgs
 *   all arguments as passed to `main`
 * @param options
 *   values of each `--name` or `--name=value` option, in order; an option without a value has an empty list
 * @param nonOptionArgs
 *   arguments that are not options, in order
 */
final case class ApplicationArguments(
  sourceArgs:    List[String],
  options:       Map[String, List[String]],
  nonOptionArgs: List[String]
) {

  /** Names of all options, without the leading `--`. */
  def optionNames: Set[String] = options.keySet

  /** Whether the option `name` was given, with or without a value. */
  def containsOption(name: String): Boolean = options.contains(name)

  /** All values of the option `name`, or an empty list when it was not given or has no value. */
  def optionValues(name: String): List[String] = options.getOrElse(name, Nil)

  /** The first value of the option `name`, if any. */
  def optionValue(name: String): Option[String] = optionValues(name).headOption
}

/** Factories for [[dev.coilix.boot.ApplicationArguments]]. */
object ApplicationArguments {

  /** Arguments of an application started without any. */
  val empty: ApplicationArguments = ApplicationArguments(Nil, Map.empty, Nil)
}
