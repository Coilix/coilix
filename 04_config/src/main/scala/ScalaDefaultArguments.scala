package dev.coilix.config

import java.lang.reflect.Method
import scala.util.Try

/**
 * Finds Scala default arguments of constructor parameters through the methods that the Scala compiler generates in the
 * companion object.
 *
 * Classes without a companion object, and parameters without a default, give `None`.
 */
final class ScalaDefaultArguments extends DefaultArguments {

  private val companionSuffix     = "$"
  private val moduleField         = "MODULE$"
  private val defaultMethodPrefix = "$lessinit$greater$default$"

  override def find(owner: Class[?], parameterIndex: Int): Option[AnyRef] =
    companionOf(owner).flatMap(companion => defaultValue(companion, parameterIndex))

  private def companionOf(owner: Class[?]): Option[AnyRef] =
    Try(Class.forName(owner.getName + companionSuffix, true, owner.getClassLoader)).toOption.flatMap(moduleOf)

  private def moduleOf(companionClass: Class[?]): Option[AnyRef] =
    Try(companionClass.getField(moduleField).get(companionClass)).toOption

  private def defaultValue(companion: AnyRef, parameterIndex: Int): Option[AnyRef] =
    defaultMethod(companion, parameterIndex).map(_.invoke(companion))

  private def defaultMethod(companion: AnyRef, parameterIndex: Int): Option[Method] =
    Try(companion.getClass.getMethod(defaultMethodPrefix + (parameterIndex + 1))).toOption
}
