package dev.coilix.common
package edit

import dev.coilix.common.edit.EditOption.NoEdit
import dev.coilix.common.edit.EditOption.SetTo
import scala.util.Try

class EditOptionTest extends munit.FunSuite {

  private val set:     EditOption[Int] = SetTo(Some(2))
  private val cleared: EditOption[Int] = SetTo(None)
  private val noEdit:  EditOption[Int] = NoEdit

  test("map transforms only a set value") {
    assertEquals(set.map(_ * 10), SetTo(Some(20)))
    assertEquals(cleared.map(_ * 10), SetTo(None))
    assertEquals(noEdit.map(_ * 10), NoEdit)
  }

  test("flatMap replaces a set value with the result of the function") {
    val half: Int => EditOption[Int] = value => if (value % 2 == 0) SetTo(Some(value / 2)) else NoEdit
    assertEquals(set.flatMap(half), SetTo(Some(1)))
    assertEquals(SetTo(Some(3)).flatMap(half), NoEdit)
    assertEquals(cleared.flatMap(half), SetTo(None))
    assertEquals(noEdit.flatMap(half), NoEdit)
  }

  test("flatten removes one level of nesting") {
    assertEquals(SetTo(Some(set)).flatten, set)
    assertEquals(SetTo(Some(noEdit)).flatten, NoEdit)
    assertEquals((SetTo(None): EditOption[EditOption[Int]]).flatten, SetTo(None))
    assertEquals((NoEdit:      EditOption[EditOption[Int]]).flatten, NoEdit)
  }

  test("fold picks the branch for each state") {
    def describe(edit: EditOption[Int]): String = edit.fold("keep", "clear")(value => s"set $value")
    assertEquals(describe(set), "set 2")
    assertEquals(describe(cleared), "clear")
    assertEquals(describe(noEdit), "keep")
  }

  test("collect and filter turn a rejected value into NoEdit") {
    assertEquals(set.collect { case 2 => "two" }, SetTo(Some("two")))
    assertEquals(set.collect { case 3 => "three" }, NoEdit)
    assertEquals(cleared.collect { case 2 => "two" }, SetTo(None))
    assertEquals(set.filter(_ > 1), set)
    assertEquals(set.filter(_ > 5), NoEdit)
    assertEquals(cleared.filter(_ > 5), cleared)
    assertEquals(noEdit.filter(_ > 5), NoEdit)
  }

  test("an edit works in a for expression with a guard") {
    val result = for {
      value <- set
      if value > 1
    } yield value + 1
    assertEquals(result, SetTo(Some(3)))
  }

  test("zip combines two edits") {
    assertEquals(set.zip(SetTo(Some("a"))), SetTo(Some((2, "a"))))
    assertEquals(set.zip(SetTo(None)), SetTo(None))
    assertEquals(cleared.zip(NoEdit), SetTo(None))
    assertEquals(set.zip(NoEdit), NoEdit)
    assertEquals(noEdit.zip(SetTo(Some("a"))), NoEdit)
  }

  test("orElse falls back only for NoEdit") {
    assertEquals(noEdit.orElse(set), set)
    assertEquals(cleared.orElse(set), cleared)
    assertEquals(set.orElse(cleared), set)
  }

  test("getOrElse returns the default unless a value is set") {
    assertEquals(set.getOrElse(0), 2)
    assertEquals(cleared.getOrElse(0), 0)
    assertEquals(noEdit.getOrElse(0), 0)
  }

  test("applyTo keeps, replaces or clears the current value") {
    assertEquals(noEdit.applyTo(Some(1)), Some(1))
    assertEquals(set.applyTo(Some(1)), Some(2))
    assertEquals(set.applyTo(None), Some(2))
    assertEquals(cleared.applyTo(Some(1)), None)
  }

  test("conversions keep or lose the difference between NoEdit and a cleared value") {
    assertEquals(set.toOption, Some(2))
    assertEquals(cleared.toOption, None)
    assertEquals(noEdit.toOption, None)
    assertEquals(set.toNestedOption, Some(Some(2)))
    assertEquals(cleared.toNestedOption, Some(None))
    assertEquals(noEdit.toNestedOption, None)
    assertEquals(set.toList, List(2))
    assertEquals(cleared.toList, Nil)
  }

  test("state predicates describe each state") {
    assertEquals(List(set, cleared, noEdit).map(_.isNoEdit), List(false, false, true))
    assertEquals(List(set, cleared, noEdit).map(_.isSet), List(true, true, false))
    assertEquals(List(set, cleared, noEdit).map(_.isDefined), List(true, false, false))
    assertEquals(List(set, cleared, noEdit).map(_.isEmpty), List(false, true, true))
  }

  test("value predicates look only at a set value") {
    assertEquals(List(set, cleared, noEdit).map(_.exists(_ == 2)), List(true, false, false))
    assertEquals(List(set, cleared, noEdit).map(_.forall(_ == 3)), List(false, true, true))
    assertEquals(List(set, cleared, noEdit).map(_.contains(2)), List(true, false, false))
  }

  test("foreach runs only for a set value") {
    val called = List(set, cleared, noEdit).map(edit => Try(edit.foreach(_ => throw IllegalStateException())).isFailure)
    assertEquals(called, List(true, false, false))
  }

  test("factory methods create each state") {
    assertEquals(EditOption(1), SetTo(Some(1)))
    assertEquals(EditOption.some(1), SetTo(Some(1)))
    assertEquals(EditOption.cleared[Int], SetTo(None))
    assertEquals(EditOption.fromOption(Some(1)), SetTo(Some(1)))
    assertEquals(EditOption.fromOption(Option.empty[Int]), SetTo(None))
    assertEquals(EditOption.fromNestedOption(Some(Some(1))), SetTo(Some(1)))
    assertEquals(EditOption.fromNestedOption(Some(Option.empty[Int])), SetTo(None))
    assertEquals(EditOption.fromNestedOption(Option.empty[Option[Int]]), NoEdit)
  }

  test("SetTo without an argument clears the field") {
    assertEquals(SetTo[Int](), SetTo(None))
  }
}
