package io.github.coilix.config

trait ScalarReader {
  def supports(targetType: Class[?]): Boolean

  def read(tree: ConfigTree, path: String, targetType: Class[?]): AnyRef
}
