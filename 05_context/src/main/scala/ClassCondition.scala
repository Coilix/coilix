package dev.coilix.context

import dev.coilix.config.Environment
import scala.util.Try

/**
 * Holds when every named class can be loaded. Read from [[dev.coilix.annotation.ConditionalOnClass]].
 *
 * @param classNames
 *   fully qualified class names
 * @param classLoader
 *   the class loader that must see the classes
 */
final case class ClassCondition(classNames: List[String], classLoader: ClassLoader) extends BeanCondition {

  override def matches(environment: Environment): Boolean =
    classNames.forall(isPresent)

  override def description: String =
    s"classes ${classNames.mkString(", ")} are on the classpath"

  private def isPresent(className: String): Boolean =
    Try(Class.forName(className, false, classLoader)).isSuccess
}
