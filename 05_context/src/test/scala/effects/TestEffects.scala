package dev.coilix.context
package effects

object TestEffects {
  val recognizer: ClassNameEffectfulResultRecognizer =
    ClassNameEffectfulResultRecognizer(Map(classOf[Later[?]].getName -> 0), Map(classOf[Scoped[?, ?]].getName -> 1))

  val scanner: ReflectiveBeanMethodScanner =
    ReflectiveBeanMethodScanner(AnnotatedParameterDependencyMapper(GenericDependencyTypeMapper()), recognizer)

  def definitions: List[BeanDefinition] =
    Definitions.of(classOf[EffectConfiguration]).filter(_.beanName == "effectConfiguration") ++
      scanner.scan(classOf[EffectConfiguration])
}
