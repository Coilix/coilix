package io.github.coilix.context

import scala.reflect.ClassTag

trait ApplicationContext {

  def getBean[T: ClassTag]: T

  def getBeanOption[T: ClassTag]: Option[T]

  def getBeansOfType[T: ClassTag]: List[T]

  def getBeanByName(beanName: String): Option[AnyRef]

  def beanNames: List[String]
}
