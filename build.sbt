ThisBuild / organization                          := "dev.coilix"
ThisBuild / versionScheme                         := Some("early-semver")
ThisBuild / mimaFailOnNoPrevious                  := false
ThisBuild / coverageMinimumStmtTotal              := 90
ThisBuild / coverageMinimumBranchTotal            := 80
ThisBuild / coverageFailOnMinimum                 := true
ThisBuild / javafmtStyle                          := com.google.googlejavaformat.java.JavaFormatterOptions.Style.AOSP
ThisBuild / javafmtFormatterCompatibleJavaVersion := 17
ThisBuild / scalaVersion                          := "3.9.0"
ThisBuild / scalacOptions ++= Seq("-no-indent", "-old-syntax", "-deprecation", "-feature", "-unchecked", "-Werror")
ThisBuild / scalacOptions ++= javaOutputVersionOptions

ThisBuild / organizationName     := "Coilix"
ThisBuild / organizationHomepage := Some(url("https://github.com/Coilix"))
ThisBuild / homepage             := Some(url("https://github.com/Coilix/coilix"))
ThisBuild / licenses             := List(License.Apache2)
ThisBuild / startYear            := Some(2026)
ThisBuild / scmInfo              := Some(
  ScmInfo(
    url("https://github.com/Coilix/coilix"),
    "scm:git:https://github.com/Coilix/coilix.git",
    "scm:git:git@github.com:Coilix/coilix.git"
  )
)
ThisBuild / developers           := List(
  Developer(
    "pawelwojcik7",
    "Paweł Wójcik",
    "79653195+pawelwojcik7@users.noreply.github.com",
    url("https://github.com/pawelwojcik7")
  )
)

ReleaseGroups.buildSettings(Seq(ReleaseGroups.core, ReleaseGroups.intellij, "data", "web", "security"))

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

lazy val javaRelease = "17"

lazy val javaOutputVersionOptions: Seq[String] = Seq("-java-output-version", javaRelease)

lazy val checkForbiddenCalls = taskKey[Unit]("Fails when Scala sources write to the console")

lazy val checkLicenses = taskKey[Unit]("Fails when a compile or runtime dependency has a license outside the allowed list")

def legalFiles(converter: xsbti.FileConverter, root: File): Seq[(HashedVirtualFileRef, String)] =
  Seq("LICENSE", "NOTICE").map(name => converter.toVirtualFile((root / name).toPath) -> s"META-INF/$name")

def isSameSeries(previous: String, current: String): Boolean =
  VersionNumber(previous).numbers.take(2) == VersionNumber(current).numbers.take(2)

def compatibilityBaseline(previous: Option[String], current: String, module: ModuleID): Set[ModuleID] =
  previous.filter(isSameSeries(_, current)).map(module.withRevision).toSet

def moduleSettings(id: String): Seq[Setting[?]] =
  Seq(
    moduleName            := s"coilix-$id",
    mimaPreviousArtifacts := compatibilityBaseline(previousStableVersion.value, version.value, projectID.value),
    checkLicenses         := Def.uncached(LicenseAudit.check(update.value, organization.value, id)),
    Compile / packageBin / mappings ++= Def.uncached(legalFiles(fileConverter.value, (ThisBuild / baseDirectory).value)),
    Compile / packageSrc / mappings ++= Def.uncached(legalFiles(fileConverter.value, (ThisBuild / baseDirectory).value))
  )

lazy val unpublished: Seq[Setting[?]] =
  Seq(
    publish / skip        := true,
    mimaPreviousArtifacts := Set.empty
  )

def isCompilerPluginOption(option: String): Boolean =
  option.startsWith("-Xplugin") || option.startsWith("-P:")

def scaladocOptions(options: Seq[String], binaryVersion: String, projectVersion: String): Seq[String] =
  options.filterNot(isCompilerPluginOption) ++
    Seq("-project", "Coilix", "-project-version", projectVersion).filter(_ => binaryVersion == "3")

def javaModule(id: String, directory: String, packageName: String): Project =
  Project(id, file(directory)).settings(moduleSettings(id)).settings(
    idePackagePrefix                     := Some(packageName),
    crossPaths                           := false,
    autoScalaLibrary                     := false,
    javafmtOnCompile                     := !insideCI.value,
    javacOptions ++= Seq("-Xlint:all", "-Werror", "--release", javaRelease),
    Compile / doc / javacOptions         := Seq("-Xdoclint:all", "-Werror"),
    Compile / unmanagedSourceDirectories := Seq((Compile / javaSource).value),
    Test / unmanagedSourceDirectories    := Seq((Test / javaSource).value)
  )

def scalaModule(id: String, directory: String, packageName: String): Project =
  Project(id, file(directory)).settings(moduleSettings(id)).settings(
    idePackagePrefix                         := Some(packageName),
    Compile / unmanagedSourceDirectories     := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories        := Seq((Test / scalaSource).value),
    libraryDependencies += Dependencies.munit % Test,
    scalafmtOnCompile                        := !insideCI.value,
    wartremoverErrors ++= forbiddenWarts,
    checkForbiddenCalls                      :=
      Def.uncached(ForbiddenCalls.check((Compile / unmanagedSources).value ++ (Test / unmanagedSources).value)),
    Compile / compile                        := Def.uncached((Compile / compile).dependsOn(checkForbiddenCalls).value),
    Compile / doc / scalacOptions            := scaladocOptions(scalacOptions.value, scalaBinaryVersion.value, version.value)
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

lazy val coreGroup: Seq[Setting[?]] = ReleaseGroups.groupSettings(ReleaseGroups.core)

lazy val coreModules: Seq[Project] = Seq(common, annotations, slf4sPlugin, config, context, boot)

def featureGroup(id: String): Seq[Setting[?]] =
  ReleaseGroups.groupSettings(id) ++
    ReleaseGroups.featureSettings(coreModules.map(_ / moduleName).join, coreModules.map(_ / baseDirectory).join)

lazy val common = scalaModule("common", "01_common", "dev.coilix.common")
  .settings(coreGroup)
  .settings(description := "Shared types and extensions for Coilix")

lazy val annotations = javaModule("annotations", "02_annotations", "dev[.]coilix[.]annotation")
  .settings(coreGroup)
  .settings(description := "Runtime annotations for Coilix components, configuration and logging")

lazy val slf4sPlugin = scalaModule("slf4s-plugin", "03_slf4s_plugin", "dev.coilix.slf4s")
  .settings(coreGroup)
  .settings(
    description                         := "Scala 3 compiler plugin that adds an SLF4J logger to classes annotated with @Slf4s",
    libraryDependencies ++= Seq(
      Dependencies.scalaCompiler(scalaVersion.value) % Provided,
      Dependencies.slf4jApi                          % Test
    ),
    Test / fork                         := true,
    coverageEnabled                     := false,
    Compile / packageBin / artifactPath :=
      fileConverter.value.toVirtualFile(((Compile / target).value / "coilix-slf4s-plugin.jar").toPath)
  )

lazy val config = slf4sModule("config", "04_config", "dev.coilix.config")
  .dependsOn(common)
  .settings(coreGroup)
  .settings(
    description := "Configuration loading, binding and component overrides for Coilix",
    libraryDependencies ++= Seq(Dependencies.typesafeConfig, Dependencies.snakeyamlEngine)
  )

lazy val context = slf4sModule("context", "05_context", "dev.coilix.context")
  .dependsOn(config)
  .settings(coreGroup)
  .settings(
    description := "Coilix IoC container",
    libraryDependencies += Dependencies.reflections
  )

lazy val data = slf4sModule("data", "06_data", "dev.coilix.data")
  .dependsOn(context)
  .settings(featureGroup("data"))
  .settings(unpublished)
  .settings(description := "Database support for Coilix")

lazy val web = slf4sModule("web", "07_web", "dev.coilix.web")
  .dependsOn(context)
  .settings(featureGroup("web"))
  .settings(unpublished)
  .settings(description := "Web endpoints for Coilix")

lazy val security = slf4sModule("security", "08_security", "dev.coilix.security")
  .dependsOn(web)
  .settings(featureGroup("security"))
  .settings(unpublished)
  .settings(description := "Security for Coilix")

lazy val boot = slf4sModule("boot", "09_boot", "dev.coilix.boot")
  .dependsOn(context)
  .settings(coreGroup)
  .settings(description := "Application startup for Coilix")

lazy val demo = slf4sModule("demo", "10_demo", "dev.coilix.demo")
  .dependsOn(boot)
  .settings(unpublished)
  .settings(
    coverageEnabled      := false,
    libraryDependencies += Dependencies.slf4jSimple,
    run / fork           := true,
    run / outputStrategy := Some(StdoutOutput)
  )

lazy val installIntellijPlugin = taskKey[File]("Copies the IntelliJ plugin jar into the local IDE plugins directory")

lazy val intellijPlugin = scalaModule("intellij-plugin", "11_intellij_plugin", "dev.coilix.intellij")
  .settings(ReleaseGroups.groupSettings(ReleaseGroups.intellij))
  .settings(unpublished)
  .settings(
    coverageEnabled                                    := false,
    scalaVersion                                       := "2.13.18",
    scalacOptions --= Seq("-no-indent", "-old-syntax") ++ javaOutputVersionOptions,
    Compile / unmanagedJars ++= Def.uncached {
      val converter = fileConverter.value
      IntellijPlugin.compileJars.map(jar => Attributed.blank(converter.toVirtualFile(jar.toPath)))
    },
    installIntellijPlugin                              := Def.uncached(IntellijPlugin.install(fileConverter.value.toPath((Compile / packageBin).value).toFile))
  )

lazy val apiSite =
  taskKey[File]("Builds the combined Scaladoc of the published modules with the Javadoc of the annotations next to it")

def documentationLine(version: String): String =
  if (version.contains("SNAPSHOT") || version.contains("+")) "next" else VersionNumber(version).numbers.take(2).mkString(".")

def javadocMapping(line: String): String =
  s"-external-mappings:dev[.]coilix[.]annotation.*::javadoc::https://docs.coilix.dev/api/$line/javadoc/"

lazy val docs = slf4sModule("docs", "12_docs", "com.example")
  .dependsOn(boot)
  .settings(unpublished)
  .settings(
    description                            := "Compiled examples and the combined API documentation of the Coilix documentation site",
    coverageEnabled                        := false,
    Compile / unmanagedSourceDirectories   := Seq(baseDirectory.value / "examples" / "next" / "scala"),
    Compile / unmanagedResourceDirectories := Seq(baseDirectory.value / "examples" / "next" / "resources"),
    libraryDependencies += Dependencies.slf4jSimple,
    run / fork                             := true,
    run / outputStrategy                   := Some(StdoutOutput),
    Test / fork                            := true,
    Test / javaOptions += s"-Dcoilix.docs.resources=${baseDirectory.value / "examples" / "next" / "resources"}",
    Compile / doc / tastyFiles             := Def.uncached(
      Seq(
        (common / Compile / tastyFiles).value,
        (slf4sPlugin / Compile / tastyFiles).value,
        (config / Compile / tastyFiles).value,
        (context / Compile / tastyFiles).value,
        (boot / Compile / tastyFiles).value
      ).flatten
    ),
    Compile / doc / dependencyClasspath ++= (slf4sPlugin / Compile / dependencyClasspath).value,
    Compile / doc / scalacOptions += javadocMapping(documentationLine(version.value)),
    apiSite                                := Def.uncached(
      ApiSite.assemble(
        (Compile / doc).value,
        (annotations / Compile / doc).value,
        target.value / "api-site"
      )
    )
  )

lazy val modules: Seq[ProjectReference] =
  Seq[ProjectReference](common, annotations, slf4sPlugin, config, context, data, web, security, boot, demo, docs) ++
    Seq[ProjectReference](intellijPlugin).filter(_ => IntellijPlugin.isAvailable)

lazy val root = (project in file("."))
  .aggregate(modules *)
  .settings(
    name           := "coilix",
    publish / skip := true
  )

addCommandAlias(
  "checkFormat",
  "; scalafmtCheckAll ; scalafmtSbtCheck ; javafmtCheckAll"
)

addCommandAlias(
  "testCoverage",
  "; clean ; coverage ; testFull ; coverageAggregate ; coverageOff"
)
