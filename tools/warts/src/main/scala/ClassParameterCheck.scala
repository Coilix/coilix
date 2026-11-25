package dev.coilix.warts

import org.wartremover.WartTraverser
import org.wartremover.WartUniverse

/**
 * Reports a public or protected method, constructor or extension method that takes the class of one of its type parameters
 * as a value: a parameter of type `Class[T]` or `Class[? <: T]` next to the type parameter `T`.
 *
 * Coilix APIs take such a class as a context bound, `[T: ClassTag]`, and get it with `dev.coilix.common.ClassTags`. A
 * `Class[?]` that is not tied to a type parameter (reflection, lists of classes in SPIs) is not reported, nor are private
 * members, members of private types, overrides, local definitions and files for which `skipped` returns `true`.
 *
 * @param skipped
 *   tells, by the path of a source file, whether the file is left out, such as test sources
 */
abstract class ClassParameterCheck(skipped: String => Boolean) extends WartTraverser {

  /** The message reported for every such parameter. */
  val message: String =
    "Take the class of a type parameter as a context bound ([T: ClassTag]) instead of a Class[T] parameter, and get it with " +
      "dev.coilix.common.ClassTags.runtimeTypeOf[T]"

  override def apply(u: WartUniverse): u.Traverser =
    new u.Traverser(this) {
      import q.reflect.*

      private val classSymbol = Symbol.requiredClass("java.lang.Class")

      override def traverseTree(tree: Tree)(owner: Symbol): Unit =
        tree match {
          case annotated if hasWartAnnotation(annotated) => ()
          case method: DefDef if isApi(method) => reportParameters(method)
          case _ => super.traverseTree(tree)(owner)
        }

      private def reportParameters(method: DefDef): Unit = {
        val typeParameters = method.paramss.collect { case TypeParamClause(types) => types.map(_.symbol) }.flatten.toSet
        method.termParamss.flatMap(_.params).filter(parameter => isBoundClass(parameter.tpt.tpe, typeParameters)).foreach {
          parameter => error(parameter.pos, message)
        }
      }

      private def isApi(method: DefDef): Boolean =
        !skipped(method.pos.sourceFile.path) && isVisible(method.symbol) && !method.symbol.flags.is(Flags.Override) &&
          method.symbol.owner.isClassDef && enclosingTypesVisible(method.symbol.owner)

      private def isVisible(symbol: Symbol): Boolean =
        !symbol.flags.is(Flags.Private) && !symbol.flags.is(Flags.Synthetic) && symbol.privateWithin.isEmpty

      private def enclosingTypesVisible(symbol: Symbol): Boolean =
        if (symbol.isNoSymbol || symbol.isPackageDef || (symbol.isTerm && !symbol.flags.is(Flags.Module))) true
        else isVisible(symbol) && enclosingTypesVisible(symbol.owner)

      private def isBoundClass(parameterType: TypeRepr, typeParameters: Set[Symbol]): Boolean =
        parameterType.dealias match {
          case AppliedType(constructor, List(argument)) if constructor.typeSymbol == classSymbol =>
            refersTo(argument, typeParameters)
          case _                                                                                 => false
        }

      private def refersTo(argument: TypeRepr, typeParameters: Set[Symbol]): Boolean =
        argument match {
          case TypeBounds(_, upper) => typeParameters(upper.typeSymbol)
          case other                => typeParameters(other.typeSymbol)
        }
    }
}
