package dev.coilix.warts

import munit.FunSuite
import org.wartremover.test.WartTestTraverser
import scala.reflect.ClassTag

final class ClassParameterCheckTest extends FunSuite {
  private val reported = List(s"[wartremover:EveryFileClassParameterCheck] ${EveryFileClassParameterCheck.message}")

  test("reports a public method that takes the class of its type parameter") {
    val result = WartTestTraverser(EveryFileClassParameterCheck) {
      final class Registry {
        def get[T](kind: Class[T]): String = kind.getName
      }
    }
    assertEquals(result.errors, reported)
  }

  test("reports a class of a subtype of the type parameter and a protected method") {
    val result = WartTestTraverser(EveryFileClassParameterCheck) {
      class Registry {
        protected def find[T](kind: Class[? <: T]): String = kind.getName
      }
    }
    assertEquals(result.errors, reported)
  }

  test("reports a public constructor that takes the class of a type parameter of its class") {
    val result = WartTestTraverser(EveryFileClassParameterCheck) {
      final class Lookup[T](val kind: Class[T])
    }
    assertEquals(result.errors, reported)
  }

  test("does not report a class without a type parameter, a context bound or private members") {
    val result = WartTestTraverser(EveryFileClassParameterCheck) {
      final class Registry {
        def components: List[Class[?]] = Nil

        def named(kind: Class[?]): String = kind.getName

        def get[T: ClassTag]: String = summon[ClassTag[T]].runtimeClass.getName

        def describe[T](kind: Class[String], value: T): String = s"${kind.getName} $value"

        private def raw[T](kind: Class[T]): String = kind.getName

        private[warts] def scoped[T](kind: Class[T]): String = raw(kind)
      }
    }
    assertEquals(result.errors, Nil)
  }

  test("does not report members of a private type, local definitions and overrides") {
    val result = WartTestTraverser(EveryFileClassParameterCheck) {
      trait Source {
        @SuppressWarnings(Array("dev.coilix.warts.EveryFileClassParameterCheck"))
        def load[T](kind: Class[T]): String
      }
      final class JavaSource extends Source {
        override def load[T](kind: Class[T]): String = {
          def local[A](inner: Class[A]): String = inner.getName
          local(kind)
        }
      }
      object Hidden {
        private final class Helper {
          def get[T](kind: Class[T]): String = kind.getName
        }
      }
    }
    assertEquals(result.errors, Nil)
  }

  test("skips files for which the check is told to skip them") {
    val result = WartTestTraverser(ClassParameterInApi) {
      final class Registry {
        def get[T](kind: Class[T]): String = kind.getName
      }
    }
    assertEquals(result.errors, Nil)
  }
}
