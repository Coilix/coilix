import sbt.*

object Dependencies {
  val typesafeConfig: ModuleID = "com.typesafe" % "config" % "1.4.9"
  val snakeyamlEngine: ModuleID = "org.snakeyaml" % "snakeyaml-engine" % "3.2"
  val reflections: ModuleID = "org.reflections" % "reflections" % "0.10.2"
  val slf4jApi: ModuleID = "org.slf4j" % "slf4j-api" % "2.0.20"
  val slf4jSimple: ModuleID = "org.slf4j" % "slf4j-simple" % "2.0.20"
  val munit: ModuleID = "org.scalameta" %% "munit" % "1.3.6"

  def scalaCompiler(scalaVersion: String): ModuleID =
    "org.scala-lang" %% "scala3-compiler" % scalaVersion
}
