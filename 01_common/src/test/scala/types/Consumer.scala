package dev.coilix.common
package types

final class Consumer(val effectRepo: Repo[Effect], val optionRepo: Repo[Option], val counts: Option[Int], val any: Option[?])
