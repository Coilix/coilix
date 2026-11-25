package dev.coilix.warts

import munit.FunSuite
import org.wartremover.test.WartTestTraverser

final class ConsoleOutputTest extends FunSuite {
  private val reported = List(s"[wartremover:ConsoleOutput] ${ConsoleOutput.message}")

  test("reports println, print and printf of Predef") {
    assertEquals(WartTestTraverser(ConsoleOutput)(println("x")).errors, reported)
    assertEquals(WartTestTraverser(ConsoleOutput)(print("x")).errors, reported)
    assertEquals(WartTestTraverser(ConsoleOutput)(printf("%s", "x")).errors, reported)
  }

  test("reports the members of scala.Console") {
    assertEquals(WartTestTraverser(ConsoleOutput)(Console.println("x")).errors, reported)
    assertEquals(WartTestTraverser(ConsoleOutput)(Console.err.println("x")).errors, reported)
  }

  test("reports System.out and System.err") {
    assertEquals(WartTestTraverser(ConsoleOutput)(System.out.println("x")).errors, reported)
    assertEquals(WartTestTraverser(ConsoleOutput)(System.err.print("x")).errors, reported)
  }

  test("does not report console methods named in text or methods of other objects with the same name") {
    val result = WartTestTraverser(ConsoleOutput) {
      val hint = "use println(\"x\") only in scripts"
      Printer.print(hint)
    }
    assertEquals(result.errors, Nil)
  }

  test("skips a definition that suppresses the wart") {
    val result = WartTestTraverser(ConsoleOutput) {
      @SuppressWarnings(Array("dev.coilix.warts.ConsoleOutput"))
      def show(): Unit = println("x")
      show()
    }
    assertEquals(result.errors, Nil)
  }
}
