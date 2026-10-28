ThisBuild / organization := "org.scaling"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.10.0"
ThisBuild / scalacOptions ++= Seq("-no-indent", "-old-syntax", "-deprecation", "-feature", "-unchecked", "-Werror")

lazy val forbiddenWarts: Seq[wartremover.Wart] = Seq(
  Wart.Var,
  Wart.Null,
  Wart.Return,
  Wart.While,
  Wart.MutableDataStructures,
  Wart.OptionPartial,
  Wart.TryPartial,
  Wart.ScalaApp
)

lazy val checkForbiddenCalls = taskKey[Unit]("Fails when Scala sources write to the console")

def javaModule(id: String, directory: String, packageName: String): Project =
  Project(id, file(directory)).settings(
    idePackagePrefix := Some(packageName),
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-Xlint:all", "-Werror"),
    Compile / unmanagedSourceDirectories := Seq((Compile / javaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / javaSource).value)
  )

def scalaModule(id: String, directory: String, packageName: String): Project =
  Project(id, file(directory)).settings(
    idePackagePrefix := Some(packageName),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    libraryDependencies += Dependencies.munit % Test,
    scalafmtOnCompile := true,
    wartremoverErrors ++= forbiddenWarts,
    checkForbiddenCalls := Def.uncached(ForbiddenCalls.check((Compile / unmanagedSources).value ++ (Test / unmanagedSources).value)),
    Compile / compile := Def.uncached((Compile / compile).dependsOn(checkForbiddenCalls).value)
  )

def slf4sModule(id: String, directory: String, packageName: String): Project =
  scalaModule(id, directory, packageName)
    .dependsOn(annotations)
    .settings(
      scalacOptions ++= Seq(
        s"-Xplugin:${fileConverter.value.toPath((slf4sPlugin / Compile / packageBin).value)}",
        "-Xplugin-require:slf4s"
      ),
      libraryDependencies ++= Seq(Dependencies.slf4jApi, Dependencies.slf4jSimple % Test)
    )

lazy val common = scalaModule("common", "01_common", "org.scaling.common")

lazy val annotations = javaModule("annotations", "02_annotations", "org.scaling.annotation")

lazy val slf4sPlugin = scalaModule("slf4s-plugin", "03_slf4s_plugin", "org.scaling.slf4s")
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.scalaCompiler(scalaVersion.value),
      Dependencies.slf4jApi % Test
    ),
    Test / fork := true
  )

lazy val config = slf4sModule("config", "04_config", "org.scaling.config")
  .dependsOn(common)
  .settings(libraryDependencies ++= Seq(Dependencies.typesafeConfig, Dependencies.snakeyamlEngine))

lazy val context = slf4sModule("context", "05_context", "org.scaling.context")
  .dependsOn(config)
  .settings(libraryDependencies += Dependencies.reflections)

lazy val data = slf4sModule("data", "06_data", "org.scaling.data")
  .dependsOn(context)

lazy val web = slf4sModule("web", "07_web", "org.scaling.web")
  .dependsOn(context)

lazy val security = slf4sModule("security", "08_security", "org.scaling.security")
  .dependsOn(web)

lazy val boot = slf4sModule("boot", "09_boot", "org.scaling.boot")
  .dependsOn(context)

lazy val demo = slf4sModule("demo", "10_demo", "org.scaling.demo")
  .dependsOn(boot)
  .settings(
    libraryDependencies += Dependencies.slf4jSimple,
    run / fork := true,
    run / outputStrategy := Some(StdoutOutput)
  )

lazy val installIntellijPlugin = taskKey[File]("Copies the IntelliJ plugin jar into the local IDE plugins directory")

lazy val intellijPlugin = scalaModule("intellij-plugin", "11_intellij_plugin", "org.scaling.intellij")
  .settings(
    scalaVersion := "2.13.18",
    scalacOptions --= Seq("-no-indent", "-old-syntax"),
    Compile / unmanagedJars ++= Def.uncached {
      val converter = fileConverter.value
      IntellijPlugin.compileJars.map(jar => Attributed.blank(converter.toVirtualFile(jar.toPath)))
    },
    installIntellijPlugin := Def.uncached(IntellijPlugin.install(fileConverter.value.toPath((Compile / packageBin).value).toFile))
  )

lazy val modules: Seq[ProjectReference] =
  Seq[ProjectReference](common, annotations, slf4sPlugin, config, context, data, web, security, boot, demo) ++
    Seq[ProjectReference](intellijPlugin).filter(_ => IntellijPlugin.isAvailable)

lazy val root = (project in file("."))
  .aggregate(modules*)
  .settings(
    name := "scaling",
    publish / skip := true
  )
