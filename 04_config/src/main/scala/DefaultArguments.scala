package dev.coilix.config

/** Finds default values of constructor parameters, used when the configuration does not set a parameter. */
trait DefaultArguments {

  /**
   * The default value of the constructor parameter at `parameterIndex` (counted from 0) of `owner`, or `None` when the
   * parameter has no default.
   */
  def find(owner: Class[?], parameterIndex: Int): Option[AnyRef]
}
