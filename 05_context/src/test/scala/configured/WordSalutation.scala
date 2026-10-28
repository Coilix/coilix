package org.scaling.context.configured

final class WordSalutation(word: String) extends Salutation {
  override def salute(name: String): String = s"$word $name"
}
