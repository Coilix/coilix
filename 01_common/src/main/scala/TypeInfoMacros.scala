package dev.coilix.common

import scala.quoted.Expr
import scala.quoted.Quotes
import scala.quoted.Type
import scala.reflect.ClassTag

/**
 * Builds a [[dev.coilix.common.TypeInfo]] from a static type.
 *
 * Primitive type arguments become `Object`, as in the generic signatures that the Scala compiler writes, so a derived type
 * equals the type read from reflection.
 */
private[common] object TypeInfoMacros {

  def derive[T: Type](using quotes: Quotes): Expr[TypeInfo[T]] = {
    import quotes.reflect.*
    val repr = TypeRepr.of[T].dealias.simplified
    if (isAbstract(repr)) fromClassTag[T]
    else {
      val genericTypeExpr = genericType(repr, repr.show)
      '{ TypeInfo.of[T]($genericTypeExpr) }
    }
  }

  private def isAbstract(using quotes: Quotes)(repr: quotes.reflect.TypeRepr): Boolean = {
    val symbol = repr.typeSymbol
    symbol.isTypeParam || symbol.isAbstractType
  }

  private def fromClassTag[T: Type](using quotes: Quotes): Expr[TypeInfo[T]] =
    Expr.summon[ClassTag[T]] match {
      case Some(tag) => '{ TypeInfo.fromClass[T]($tag.runtimeClass) }
      case None      =>
        quotes.reflect.report.errorAndAbort(
          s"No TypeInfo for the abstract type ${Type.show[T]}. Add a TypeInfo or ClassTag context bound for it"
        )
    }

  private def genericType(using quotes: Quotes)(repr: quotes.reflect.TypeRepr, root: String): Expr[GenericType] = {
    import quotes.reflect.*
    repr.dealias.simplified match {
      case AppliedType(constructor, arguments) => concrete(runtimeClassOf(constructor, root), arguments.map(argument(_, root)))
      case lambda: TypeLambda => etaExpanded(lambda).map(genericType(_, root)).getOrElse(typeLambda(lambda, root))
      case other => concrete(runtimeClassOf(other, root), Nil)
    }
  }

  private def concrete(using Quotes)(rawClass: Expr[Class[?]], arguments: List[Expr[GenericType]]): Expr[GenericType] = {
    val argumentList = Expr.ofList(arguments)
    '{ GenericType.Concrete($rawClass, $argumentList) }
  }

  private def argument(using quotes: Quotes)(repr: quotes.reflect.TypeRepr, root: String): Expr[GenericType] = {
    import quotes.reflect.*
    repr match {
      case _: TypeBounds => '{ GenericType.Wildcard }
      case primitive if isPrimitive(primitive) => '{ GenericType.Concrete(classOf[Object], Nil) }
      case lambda: TypeLambda => etaExpanded(lambda).map(genericType(_, root)).getOrElse(typeLambda(lambda, root))
      case other => genericType(other, root)
    }
  }

  private def isPrimitive(using quotes: Quotes)(repr: quotes.reflect.TypeRepr): Boolean = {
    import quotes.reflect.*
    List(TypeRepr.of[AnyVal], TypeRepr.of[Any]).exists(repr =:= _) ||
    List(TypeRepr.of[Int], TypeRepr.of[Long], TypeRepr.of[Double], TypeRepr.of[Float], TypeRepr.of[Short], TypeRepr.of[Byte])
      .exists(repr =:= _) ||
    List(TypeRepr.of[Char], TypeRepr.of[Boolean], TypeRepr.of[Unit]).exists(repr =:= _)
  }

  private def runtimeClassOf(using quotes: Quotes)(repr: quotes.reflect.TypeRepr, root: String): Expr[Class[?]] = {
    import quotes.reflect.*
    val symbol = repr.typeSymbol
    if (!symbol.isClassDef) report.errorAndAbort(s"Cannot read the class of ${repr.show} in $root")
    Literal(ClassOfConstant(symbol.typeRef.appliedTo(symbol.declaredTypes.filter(_.isTypeParam).map(_ => TypeBounds.empty))))
      .asExprOf[Class[?]]
  }

  private def etaExpanded(using quotes: Quotes)(lambda: quotes.reflect.TypeLambda): Option[quotes.reflect.TypeRepr] = {
    import quotes.reflect.*
    lambda.resType match {
      case AppliedType(constructor, arguments) if appliesParametersInOrder(lambda, arguments) => Some(constructor)
      case _                                                                                  => None
    }
  }

  private def appliesParametersInOrder(using
    quotes: Quotes
  )(lambda: quotes.reflect.TypeLambda, arguments: List[quotes.reflect.TypeRepr]): Boolean =
    arguments.size == lambda.paramNames.size && arguments.zipWithIndex.forall((argument, index) => argument =:= lambda.param(index))

  private def typeLambda(using quotes: Quotes)(lambda: quotes.reflect.TypeLambda, root: String): Nothing =
    quotes.reflect.report.errorAndAbort(
      s"$root contains the type lambda ${lambda.show}, which the JVM keeps only as its class. Declare a named class for it"
    )
}
