package org.scaling.config

trait EnvironmentLoader {
  def load(overrides: Map[String, String]): Environment
}

object EnvironmentLoader {
  def default: EnvironmentLoader =
    ClasspathEnvironmentLoader("application", ConfigFormat.byPrecedence, Thread.currentThread().getContextClassLoader)
}
