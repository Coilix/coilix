package dev.coilix.warts

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite
import org.wartremover.test.WartTestTraverser

final class ThreadPrimitivesTest extends FunSuite {
  private val reported = s"[wartremover:ThreadPrimitives] ${ThreadPrimitives.message}"

  test("reports synchronized, wait, notify and notifyAll") {
    val lock = Object()
    assertEquals(WartTestTraverser(ThreadPrimitives)(lock.synchronized(1)).errors, List(reported))
    assertEquals(WartTestTraverser(ThreadPrimitives)(lock.wait()).errors, List(reported))
    assertEquals(WartTestTraverser(ThreadPrimitives)(lock.notify()).errors, List(reported))
    assertEquals(WartTestTraverser(ThreadPrimitives)(lock.notifyAll()).errors, List(reported))
  }

  test("reports creating and using the classes of java.util.concurrent.atomic") {
    assertEquals(WartTestTraverser(ThreadPrimitives)(AtomicInteger(1)).errors, List(reported))
    val counter = AtomicReference(List.empty[String])
    assertEquals(WartTestTraverser(ThreadPrimitives)(counter.get()).errors, List(reported))
  }

  test("reports Thread.sleep") {
    assertEquals(WartTestTraverser(ThreadPrimitives)(Thread.sleep(1)).errors, List(reported))
  }

  test("does not report the names in text or other methods of Object and Thread") {
    val result = WartTestTraverser(ThreadPrimitives) {
      val hint = "never call Thread.sleep or synchronized"
      (hint.hashCode, Thread.currentThread.getName)
    }
    assertEquals(result.errors, Nil)
  }

  test("skips a definition that suppresses the wart") {
    val result = WartTestTraverser(ThreadPrimitives) {
      @SuppressWarnings(Array("dev.coilix.warts.ThreadPrimitives"))
      def counted(): Int = AtomicInteger(1).incrementAndGet()
      counted()
    }
    assertEquals(result.errors, Nil)
  }
}
