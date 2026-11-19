package dev.coilix.common

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

/**
 * A type with its type arguments, as the JVM generic signatures describe it.
 *
 * It is read from reflection with [[dev.coilix.common.GenericType.of]] or from a static type with
 * [[dev.coilix.common.TypeInfo]], so `Repository[IO]` and `Repository[Option]` are different types although they share a class.
 */
enum GenericType {

  /** A class with its type arguments, empty when the class is not generic or its arguments are not known. */
  case Concrete(rawClass: Class[?], arguments: List[GenericType])

  /** A type variable of a generic class or method, such as `F` in `SqlRepository[F[_]]`, not bound to a type. */
  case Variable(name: String)

  /** An unknown type argument, such as `?` in `Option[?]`, which matches any type. */
  case Wildcard

  /** The class of this type, or `Object` for a variable or a wildcard. */
  def erasure: Class[?] =
    this match {
      case Concrete(rawClass, _) => rawClass
      case _                     => classOf[AnyRef]
    }

  /** The type variables in this type and its type arguments, in order of appearance and without duplicates. */
  def variables: List[String] =
    this match {
      case Concrete(_, arguments) => arguments.flatMap(_.variables).distinct
      case Variable(name)         => List(name)
      case Wildcard               => Nil
    }

  /**
   * Whether a value of type `candidate` can be used where this type is expected.
   *
   * The classes must be assignable. When this type has type arguments, `candidate` must extend this class with equal type
   * arguments, where a wildcard in this type matches any argument and a type variable in `candidate` matches nothing. Without
   * type arguments, only the classes are compared.
   */
  def isAssignableFrom(candidate: GenericType): Boolean =
    (this, candidate) match {
      case (Wildcard, _)                                   => true
      case (Concrete(rawClass, Nil), Concrete(other, _))   => rawClass.isAssignableFrom(other)
      case (Concrete(rawClass, arguments), Concrete(_, _)) => candidate.asSuper(rawClass).exists(argumentsMatch(arguments))
      case _                                               => false
    }

  /**
   * This type seen as its supertype `target`, with the type arguments of this type substituted into it, or `None` when it does
   * not extend `target`. Type variables of a class used without type arguments stay unbound.
   */
  def asSuper(target: Class[?]): Option[GenericType] =
    this match {
      case Concrete(rawClass, _) if rawClass == target                        => Some(this)
      case Concrete(rawClass, arguments) if target.isAssignableFrom(rawClass) =>
        GenericType.supertypes(rawClass).map(_.substitute(bindings(rawClass, arguments))).flatMap(_.asSuper(target)).headOption
      case _                                                                  => None
    }

  /** This type with each type variable named in `bindings` replaced by its bound type. */
  def substitute(bindings: Map[String, GenericType]): GenericType =
    this match {
      case Concrete(rawClass, arguments) => Concrete(rawClass, arguments.map(_.substitute(bindings)))
      case Variable(name)                => bindings.getOrElse(name, this)
      case Wildcard                      => this
    }

  /** The type as written in Scala, with fully qualified class names, such as `com.example.Repository[cats.effect.IO]`. */
  override def toString: String =
    this match {
      case Concrete(rawClass, Nil)       => rawClass.getName
      case Concrete(rawClass, arguments) => s"${rawClass.getName}[${arguments.mkString(", ")}]"
      case Variable(name)                => name
      case Wildcard                      => "?"
    }

  private def argumentsMatch(expected: List[GenericType])(actual: GenericType): Boolean =
    actual match {
      case Concrete(_, arguments) => arguments.size == expected.size && expected.zip(arguments).forall(argumentMatches)
      case _                      => false
    }

  private def argumentMatches(expected: GenericType, actual: GenericType): Boolean =
    expected == Wildcard || expected == actual

  private def bindings(rawClass: Class[?], arguments: List[GenericType]): Map[String, GenericType] =
    rawClass.getTypeParameters.toList.map(_.getName).zip(arguments).toMap
}

/** Reading of [[dev.coilix.common.GenericType]] from Java reflection. */
object GenericType {

  /**
   * The generic type of a reflected type, such as a constructor parameter, a method result or a supertype.
   *
   * Scala erases a primitive type argument to `Object`, so `Option[Int]` is read as `Option[Object]`.
   */
  def of(reflected: Type): GenericType =
    reflected match {
      case rawClass:      Class[?]          => Concrete(rawClass, Nil)
      case parameterized: ParameterizedType => parameterizedOf(parameterized)
      case variable:      TypeVariable[?]   => Variable(variable.getName)
      case _:             WildcardType      => Wildcard
      case array:         GenericArrayType  => Concrete(arrayClassOf(of(array.getGenericComponentType)), Nil)
      case _ => Wildcard
    }

  private def parameterizedOf(parameterized: ParameterizedType): GenericType =
    Concrete(of(parameterized.getRawType).erasure, parameterized.getActualTypeArguments.toList.map(of))

  private def arrayClassOf(component: GenericType): Class[?] =
    java.lang.reflect.Array.newInstance(component.erasure, 0).getClass

  private def supertypes(rawClass: Class[?]): List[GenericType] =
    (Option(rawClass.getGenericSuperclass).toList ++ rawClass.getGenericInterfaces.toList).map(of)
}
