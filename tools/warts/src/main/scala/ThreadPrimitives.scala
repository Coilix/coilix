package dev.coilix.warts

import org.wartremover.WartTraverser
import org.wartremover.WartUniverse

/**
 * Reports the thread primitives that the runtime modules of Coilix replace with cats-effect: `synchronized`, `wait`,
 * `notify` and `notifyAll`, the classes of `java.util.concurrent.atomic` and `Thread.sleep`.
 *
 * Code on fibers uses `Ref`, `Deferred`, `Queue` and `IO.sleep` instead, which do not block a thread of the compute pool. Uses
 * are recognized by their symbol, so the names in text or in Scaladoc are not reported. Code that runs on a thread outside
 * the pool, such as a thread factory or the handler of fatal errors, is annotated with
 * `@SuppressWarnings(Array("dev.coilix.warts.ThreadPrimitives"))` and says in its Scaladoc why.
 */
object ThreadPrimitives extends WartTraverser {

  /** The message reported for every use of a thread primitive. */
  val message: String =
    "Thread primitives are forbidden in runtime modules, use Ref, Deferred, Queue or IO.sleep of cats-effect"

  private val objectOwners = Set("java.lang.Object", "scala.Any", "scala.AnyRef")

  private val monitorMethods = Set("synchronized", "wait", "notify", "notifyAll")

  private val threadOwners = Set("java.lang.Thread", "java.lang.Thread$")

  private val atomicPackage = "java.util.concurrent.atomic"

  override def apply(u: WartUniverse): u.Traverser =
    new u.Traverser(this) {
      import q.reflect.*

      override def traverseTree(tree: Tree)(owner: Symbol): Unit =
        tree match {
          case annotated if hasWartAnnotation(annotated) => ()
          case reference: Ident if forbidden(reference.symbol)  => error(reference.pos, message)
          case reference: Select if forbidden(reference.symbol) => error(reference.pos, message)
          case _ => super.traverseTree(tree)(owner)
        }

      private def forbidden(symbol: Symbol): Boolean =
        !symbol.isNoSymbol && isPrimitive(symbol.name, symbol.maybeOwner.fullName)

      private def isPrimitive(name: String, owner: String): Boolean = (objectOwners(owner) && monitorMethods(name)) ||
        (threadOwners(owner) && name == "sleep") ||
        owner.startsWith(s"$atomicPackage.")
    }
}
