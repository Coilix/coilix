import sbt.*

object Dependencies {
  val typesafeConfig:  ModuleID = "com.typesafe"    % "config"            % "1.4.9"
  val snakeyamlEngine: ModuleID = "org.snakeyaml"   % "snakeyaml-engine"  % "3.2"
  val reflections:     ModuleID = "org.reflections" % "reflections"       % "0.10.2"
  val slf4jApi:        ModuleID = "org.slf4j"       % "slf4j-api"         % "2.0.20"
  val slf4jSimple:     ModuleID = "org.slf4j"       % "slf4j-simple"      % "2.0.20"
  val munit:           ModuleID = "org.scalameta"  %% "munit"             % "1.3.6"
  val catsEffect:      ModuleID = "org.typelevel"  %% "cats-effect"       % "3.7.1"
  val munitCatsEffect: ModuleID = "org.typelevel"  %% "munit-cats-effect" % "2.2.1"
  val fs2Core:         ModuleID = "co.fs2"         %% "fs2-core"          % "3.16.0"

  /** Log4j 2 behind SLF4J, with a working MDC, for the demo and the tests of MDC propagation. */
  val log4jSlf4j: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.26.1",
    "org.apache.logging.log4j" % "log4j-core"        % "2.26.1"
  )

  /**
   * The wartremover library for the custom warts in tools/warts, in the version of the sbt-wartremover plugin from
   * project/plugins.sbt and for the exact Scala version, because the warts run inside the compiler.
   */
  val wartremover: ModuleID = ("org.wartremover" % "wartremover" % "3.6.3").cross(CrossVersion.full)

  def scalaCompiler(scalaVersion: String): ModuleID =
    "org.scala-lang" %% "scala3-compiler" % scalaVersion
}
