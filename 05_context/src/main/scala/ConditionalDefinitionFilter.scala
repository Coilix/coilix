package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment

/**
 * Checks the [[dev.coilix.context.BeanConditions]] of each definition in two passes.
 *
 * The first pass leaves out definitions with a condition that does not hold. The second pass leaves out definitions marked
 * [[dev.coilix.annotation.ConditionalOnMissingBean]] when a definition kept in the first pass without that mark, or an
 * existing bean, has the same type or a subtype, or a subtype of one of the types named in the annotation. Each left-out definition is logged at `DEBUG` level with the reason. This is
 * the default [[dev.coilix.context.DefinitionFilter]], selected when `coilix.context.definition-filter` is not set.
 *
 * @param environment
 *   the configuration the conditions are checked against
 */
@Slf4s
case class ConditionalDefinitionFilter(environment: Environment) extends DefinitionFilter {

  override def filter(definitions: List[BeanDefinition], existing: List[Class[?]]): List[BeanDefinition] = {
    val matching  = definitions.filter(conditionsHold)
    val providers = existing ++ matching.filterNot(_.conditions.onlyIfMissing).map(_.beanClass)
    matching.filter(definition => !definition.conditions.onlyIfMissing || isMissing(definition, providers))
  }

  private def conditionsHold(definition: BeanDefinition): Boolean =
    definition.conditions.conditions.find(!_.matches(environment)) match {
      case Some(failed) => leftOut(definition, s"it requires that ${failed.description}")
      case None         => true
    }

  private def isMissing(definition: BeanDefinition, providers: List[Class[?]]): Boolean =
    providers.find(provider => missingTypesOf(definition).exists(_.isAssignableFrom(provider))) match {
      case Some(provider) => leftOut(definition, s"bean of type ${provider.getName} already exists")
      case None           => true
    }

  private def missingTypesOf(definition: BeanDefinition): List[Class[?]] =
    if (definition.conditions.missingTypes.isEmpty) List(definition.beanClass) else definition.conditions.missingTypes

  private def leftOut(definition: BeanDefinition, reason: String): Boolean = {
    log.debug(s"Leaving out bean ${definition.beanName} because $reason")
    false
  }
}
