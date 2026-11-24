package dev.coilix.config
package enums

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated
import java.lang.reflect.Field
import java.util.Locale
import scala.util.Try

/**
 * Lists the cases of Scala 3 enums annotated with [[dev.coilix.annotation.Enumerated]] through reflection.
 *
 * The enum may have only cases without parameters, because only those have a fixed set of instances. The name of a
 * case comes from [[dev.coilix.annotation.EnumName]] on the case, or from the case name in the code. This is the default
 * resolver, returned by [[dev.coilix.config.enums.EnumResolver.default]].
 */
final class ReflectiveEnumResolver extends EnumResolver {

  override def isEnum(candidate: Class[?]): Boolean =
    candidate.isAnnotationPresent(classOf[Enumerated])

  override def resolve(enumType: Class[?]): EnumDefinition =
    if (!isEnum(enumType)) throw invalid(enumType, s"is not annotated with @${classOf[Enumerated].getSimpleName}")
    else unique(EnumDefinition(enumType, casesOf(enumType), enumType.getAnnotation(classOf[Enumerated]).ignoreCase))

  private def casesOf(enumType: Class[?]): List[EnumCase] =
    if (classOf[scala.reflect.Enum].isAssignableFrom(enumType)) scalaCases(enumType)
    else throw invalid(enumType, "must be a Scala 3 enum")

  private def scalaCases(enumType: Class[?]): List[EnumCase] = {
    val companion = Class.forName(s"${enumType.getName}$$", false, enumType.getClassLoader)
    val module    = companion.getField("MODULE$").get(companion)
    val fields    = caseFields(enumType, companion)
    scalaValues(enumType).map(value => scalaCase(value, fields, module))
  }

  private def scalaValues(enumType: Class[?]): List[AnyRef] =
    Try(enumType.getMethod("values")).toOption
      .map(_.invoke(enumType))
      .collect { case values: Array[AnyRef] => values.toList }
      .getOrElse(throw invalid(enumType, "must have only cases without parameters"))

  private def caseFields(enumType: Class[?], companion: Class[?]): List[Field] =
    companion.getDeclaredFields.toList.filter(field => enumType.isAssignableFrom(field.getType) && field.trySetAccessible())

  private def scalaCase(value: AnyRef, fields: List[Field], module: AnyRef): EnumCase =
    fields
      .find(_.get(module).eq(value))
      .fold(EnumCase(value, value.toString))(field => named(value, field.getName, field))

  private def named(value: AnyRef, codeName: String, field: Field): EnumCase =
    Option(field.getAnnotation(classOf[EnumName]))
      .fold(EnumCase(value, codeName))(annotation => EnumCase(value, annotation.value, annotation.aliases.toList))

  private def unique(definition: EnumDefinition): EnumDefinition =
    duplicates(definition) match {
      case Nil        => definition
      case duplicated => throw invalid(definition.enumType, s"has several cases named ${duplicated.mkString(", ")}")
    }

  private def duplicates(definition: EnumDefinition): List[String] =
    definition.cases
      .flatMap(_.allNames)
      .groupBy(name => if (definition.ignoreCase) name.toLowerCase(Locale.ROOT) else name)
      .collect { case (name, occurrences) if occurrences.sizeIs > 1 => name }
      .toList
      .sorted

  private def invalid(enumType: Class[?], problem: String): InvalidEnumException =
    InvalidEnumException(s"Enum ${enumType.getName} $problem")
}
