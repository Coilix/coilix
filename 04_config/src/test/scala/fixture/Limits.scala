package dev.coilix.config
package fixture

final case class Limits(maxSize: Long, ratios: Map[String, Double], tags: Set[String], levels: Vector[Int])
