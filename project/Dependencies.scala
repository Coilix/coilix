import sbt.*

object Dependencies {
  val typesafeConfig:  ModuleID = "com.typesafe"    % "config"           % "1.4.9"
  val snakeyamlEngine: ModuleID = "org.snakeyaml"   % "snakeyaml-engine" % "3.2"
  val reflections:     ModuleID = "org.reflections" % "reflections"      % "0.10.2"
  val slf4jApi:        ModuleID = "org.slf4j"       % "slf4j-api"        % "2.0.20"
  val slf4jSimple:     ModuleID = "org.slf4j"       % "slf4j-simple"     % "2.0.20"
  val munit:           ModuleID = "org.scalameta"  %% "munit"            % "1.3.6"
  val catsEffect:      ModuleID = "org.typelevel"  %% "cats-effect"      % "3.7.1"

  /** Log4j 2 behind SLF4J, with a working MDC, for the demo and the tests of MDC propagation. */
  val log4jSlf4j: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.26.1",
    "org.apache.logging.log4j" % "log4j-core"        % "2.26.1"
  )

  def scalaCompiler(scalaVersion: String): ModuleID =
    "org.scala-lang" %% "scala3-compiler" % scalaVersion
}
