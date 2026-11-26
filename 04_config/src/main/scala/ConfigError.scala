package dev.coilix.config

/**
 * A predictable reason why a configuration value cannot be read, bound or loaded.
 *
 * It is the left side of the reading methods of [[dev.coilix.config.ConfigTree]], [[dev.coilix.config.ScalarReader]],
 * [[dev.coilix.config.ConfigBinder]], [[dev.coilix.config.EnvironmentLoader]] and of the `Either` methods of
 * [[dev.coilix.config.Environment]] and [[dev.coilix.config.ComponentProvider]]. The caller decides what to do with it: the
 * boot module wraps it in its own startup error, and the throwing methods such as [[dev.coilix.config.Environment.bind]] throw
 * its [[dev.coilix.config.ConfigError.exception]].
 */
enum ConfigError {

  /**
   * A configuration file is not valid in its format.
   *
   * @param source
   *   the location of the file
   * @param reason
   *   the problem reported by the parser
   */
  case Unparsable(source: String, reason: String)

  /**
   * A substitution such as `${other.key}` refers to a key that no source defines.
   *
   * @param reason
   *   the problem reported by the configuration library
   */
  case Unresolved(reason: String)

  /**
   * The value at a path has a different shape than the one that was read, for example a section read as a number.
   *
   * @param path
   *   the configuration path
   * @param reason
   *   the problem reported by the configuration library
   */
  case Unreadable(path: String, reason: String)

  /**
   * A required value is not set.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the type the value was bound to
   */
  case Missing(path: String, target: Class[?])

  /**
   * The [[dev.coilix.config.ScalarReader]] cannot read the requested type.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the requested type
   */
  case UnsupportedScalar(path: String, target: Class[?])

  /**
   * The value at a path cannot be bound to the requested type, for example a single value bound to a class.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the requested type
   */
  case UnsupportedTarget(path: String, target: Class[?])

  /**
   * The requested collection type is none of `List`, `Vector` and `Set` or their supertypes.
   *
   * @param path
   *   the configuration path
   * @param collection
   *   the requested collection class
   */
  case UnsupportedCollection(path: String, collection: Class[?])

  /**
   * A value type or an enum rejects the configured value.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the value type or enum
   * @param reason
   *   why the value is rejected
   */
  case InvalidValue(path: String, target: Class[?], reason: String)

  /**
   * The class named by a `class` key cannot be loaded.
   *
   * @param path
   *   the path of the section with the `class` key
   * @param className
   *   the configured class name
   */
  case UnknownClass(path: String, className: String)

  /**
   * The class named by a `class` key does not extend the requested type.
   *
   * @param path
   *   the path of the section with the `class` key
   * @param configured
   *   the configured class
   * @param target
   *   the requested type
   */
  case IncompatibleClass(path: String, configured: Class[?], target: Class[?])

  /**
   * A section is bound to a trait or an abstract class without a `class` key.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the abstract type
   */
  case AbstractTarget(path: String, target: Class[?])

  /**
   * The class bound from a section has several public constructors and none of them is marked `@Autowired`.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the class
   */
  case AmbiguousConstructor(path: String, target: Class[?])

  /**
   * The constructor of the class bound from a section throws.
   *
   * @param path
   *   the configuration path
   * @param target
   *   the class
   * @param cause
   *   the exception thrown by the constructor
   */
  case CreationFailed(path: String, target: Class[?], cause: Throwable)

  /**
   * The value read by [[dev.coilix.config.Environment.lookup]] has a different type than the requested one.
   *
   * @param path
   *   the configuration path
   * @param actual
   *   the class of the value
   * @param target
   *   the requested type
   */
  case TypeMismatch(path: String, actual: Class[?], target: Class[?])

  /** The description of this error, naming the configuration path and what was expected. */
  def message: String =
    this match {
      case Unparsable(source, reason)                 => s"Cannot parse configuration $source: $reason"
      case Unresolved(reason)                         => s"Cannot resolve configuration: $reason"
      case Unreadable(path, reason)                   => s"Cannot read configuration '$path': $reason"
      case Missing(path, target)                      => s"Missing configuration '$path' (expected ${target.getName})"
      case UnsupportedScalar(path, target)            => s"Cannot read configuration '$path' as ${target.getName}"
      case UnsupportedTarget(path, target)            => s"Cannot bind configuration '$path' to ${target.getName}"
      case UnsupportedCollection(path, collection)    => s"Cannot bind configuration '$path' to collection ${collection.getName}"
      case InvalidValue(path, target, reason)         => s"Cannot bind configuration '$path' to ${target.getName}: $reason"
      case UnknownClass(path, className)              => s"Class '$className' configured at '$path.class' cannot be loaded"
      case IncompatibleClass(path, configured, target) => incompatible(path, configured, target)
      case AbstractTarget(path, target)               => abstractTarget(path, target)
      case AmbiguousConstructor(path, target)         => s"${target.getName} bound at '$path' needs one public constructor or one marked @Autowired"
      case CreationFailed(path, target, cause)        => s"Cannot create ${target.getName} from configuration '$path': $cause"
      case TypeMismatch(path, actual, target)         => s"Configuration '$path' is ${actual.getName}, not ${target.getName}"
    }

  /** The exception that reports this error to code that throws. */
  def exception: ConfigurationException = ConfigurationException(message)

  private def incompatible(path: String, configured: Class[?], target: Class[?]): String =
    s"Class '${configured.getName}' configured at '$path.class' is not a ${target.getName}"

  private def abstractTarget(path: String, target: Class[?]): String =
    s"Configuration '$path' binds to abstract type ${target.getName}, set '$path.class' to a concrete class"
}

/** Throwing a [[dev.coilix.config.ConfigError]] for callers that cannot handle it. */
object ConfigError {

  extension [A](result: Either[ConfigError, A]) {

    /** The value of `result`, throwing the [[dev.coilix.config.ConfigError.exception]] of an error. */
    def orThrow: A = result.fold(error => throw error.exception, identity)
  }
}
