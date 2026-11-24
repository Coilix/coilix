package dev.coilix.warts

import org.wartremover.WartTraverser
import org.wartremover.WartUniverse

/**
 * Reports writing to the console, which Coilix code does through `@Slf4s` logging instead.
 *
 * It reports the `println`, `print` and `printf` methods of `scala.Predef`, every member of `scala.Console` and the `System.out`
 * and `System.err` streams. Calls are recognized by their symbol, so text such as `"use println("` in a string or in Scaladoc is
 * not reported. A definition annotated with `@SuppressWarnings(Array("dev.coilix.warts.ConsoleOutput"))` is skipped.
 */
object ConsoleOutput extends WartTraverser {

  /** The message reported for every write to the console. */
  val message: String = "Console output is forbidden, use @Slf4s logging (log.info, log.warn, ...)"

  private val predefOwners = Set("scala.Predef", "scala.Predef$")

  private val predefMethods = Set("println", "print", "printf")

  private val consoleOwners = Set("scala.Console", "scala.Console$")

  private val systemOwners = Set("java.lang.System", "java.lang.System$")

  private val systemStreams = Set("out", "err")

  override def apply(u: WartUniverse): u.Traverser =
    new u.Traverser(this) {
      import q.reflect.*

      override def traverseTree(tree: Tree)(owner: Symbol): Unit =
        tree match {
          case annotated if hasWartAnnotation(annotated) => ()
          case reference: Ident if writesToConsole(reference.symbol)  => error(reference.pos, message)
          case reference: Select if writesToConsole(reference.symbol) => error(reference.pos, message)
          case _ => super.traverseTree(tree)(owner)
        }

      private def writesToConsole(symbol: Symbol): Boolean =
        !symbol.isNoSymbol && isConsoleMember(symbol.name, symbol.maybeOwner.fullName)

      private def isConsoleMember(name: String, owner: String): Boolean = (predefOwners(owner) && predefMethods(name)) ||
        consoleOwners(owner) || (systemOwners(owner) && systemStreams(name))
    }
}
