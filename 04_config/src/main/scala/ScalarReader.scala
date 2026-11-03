package dev.coilix.config

/** Reads single configuration values, such as numbers and durations, as a requested type. */
trait ScalarReader {

  /** Whether values can be read as `targetType`. */
  def supports(targetType: Class[?]): Boolean

  /**
   * Reads the value at `path` as `targetType`, boxing primitive types.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if `targetType` is not supported or the value cannot be converted
   */
  def read(tree: ConfigTree, path: String, targetType: Class[?]): AnyRef
}
