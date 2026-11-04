import java.util.Date
import sbt.*
import sbt.Keys.*
import sbtdynver.DynVer
import sbtdynver.DynVerPlugin.autoImport.*
import sbtdynver.GitDescribeOutput
import scala.sys.process.Process
import scala.util.Try

object ReleaseGroups {
  final case class GroupState(describe: Option[GitDescribeOutput], previous: Option[GitDescribeOutput])

  val core: String = "core"

  val intellij: String = "intellij"

  val releaseGroups = settingKey[Seq[String]]("Release groups of the build, each with its own <group>-vX.Y.Z tags")

  val releaseGroupStates =
    settingKey[Map[String, GroupState]]("Git describe output of every release group, read once for the whole build")

  val releaseGroup = settingKey[String]("Release group whose tags give this module its version")

  val releasedCoreVersion =
    settingKey[Option[String]]("Version of the last core release reachable from the current commit")

  val checkReleasedCore =
    taskKey[Unit]("Fails when a feature module release would depend on core code that is not released")

  val releaseGroupVariable: String = "RELEASE_GROUP"

  val releaseGroupFilter =
    settingKey[Option[String]]("Release group named by RELEASE_GROUP, the only group that publishes when it is set")

  val releaseVerify: Command =
    Command.args("releaseVerify", "<group> <version>")((state, arguments) => verifyCommands(state, arguments) ::: state)

  def tagPrefix(group: String): String = s"$group-v"

  def buildSettings(groups: Seq[String]): Seq[Setting[?]] =
    Seq(
      ThisBuild / releaseGroups                  := groups,
      ThisBuild / dynverTagPrefix                := tagPrefix(core),
      ThisBuild / releaseGroupStates             :=
        groups
          .map(group =>
            group -> readState(
              (ThisBuild / baseDirectory).value,
              (ThisBuild / dynverSeparator).value,
              (ThisBuild / dynverCurrentDate).value,
              group
            )
          )
          .toMap,
      ThisBuild / dynverGitDescribeOutput        := stateOf((ThisBuild / releaseGroupStates).value, core).describe,
      ThisBuild / dynverGitPreviousStableVersion := stateOf((ThisBuild / releaseGroupStates).value, core).previous,
      ThisBuild / releasedCoreVersion            := lastRelease((ThisBuild / baseDirectory).value, core),
      ThisBuild / releaseGroupFilter             := sys.env.get(releaseGroupVariable).filter(_.nonEmpty),
      Global / commands += releaseVerify
    )

  def groupSettings(group: String): Seq[Setting[?]] =
    Seq(
      releaseGroup                   := group,
      publish / skip                 := (publish / skip).value || (ThisBuild / releaseGroupFilter).value.exists(_ != group),
      dynverGitDescribeOutput        := stateOf((ThisBuild / releaseGroupStates).value, group).describe,
      dynverGitPreviousStableVersion := stateOf((ThisBuild / releaseGroupStates).value, group).previous,
      version                        :=
        dynverGitDescribeOutput.value
          .getVersion(dynverCurrentDate.value, dynverSeparator.value, dynverSonatypeSnapshots.value),
      isSnapshot                     := dynverGitDescribeOutput.value.isSnapshot,
      isVersionStable                := dynverGitDescribeOutput.value.isVersionStable,
      previousStableVersion          := dynverGitPreviousStableVersion.value.previousVersion
    )

  def featureSettings(coreNames: Def.Initialize[Seq[String]], coreDirectories: Def.Initialize[Seq[File]]): Seq[Setting[?]] =
    Seq(
      projectDependencies := Def.uncached(
        releasedDependencies(
          projectDependencies.value,
          coreNames.value.toSet,
          (ThisBuild / releasedCoreVersion).value,
          isSnapshot.value
        )
      ),
      checkReleasedCore   := Def.uncached(
        verifyReleasedCore(
          name.value,
          isSnapshot.value,
          (ThisBuild / releasedCoreVersion).value,
          (ThisBuild / baseDirectory).value,
          coreDirectories.value
        )
      )
    )

  def lastRelease(base: File, group: String): Option[String] =
    git(base, "describe", "--tags", "--abbrev=0", "--match", s"${tagPrefix(group)}[0-9]*", "HEAD")
      .map(_.stripPrefix(tagPrefix(group)))

  def releasedDependencies(
    dependencies: Seq[ModuleID],
    coreNames:    Set[String],
    released:     Option[String],
    snapshot:     Boolean
  ): Seq[ModuleID] =
    released.filter(_ => !snapshot).fold(dependencies)(coreVersion => dependencies.map(onCore(_, coreNames, coreVersion)))

  private def verifyCommands(state: State, arguments: Seq[String]): List[String] =
    arguments match {
      case Seq(group, expected) => groupChecks(state, Project.extract(state), group, expected)
      case _                    => fail("Usage: releaseVerify <group> <version>")
    }

  private def groupChecks(state: State, extracted: Extracted, group: String, expected: String): List[String] = {
    if (!extracted.get(ThisBuild / releaseGroups).contains(group)) {
      fail(s"Unknown release group $group, the groups are ${extracted.get(ThisBuild / releaseGroups).mkString(", ")}")
    }
    val modules    = publishedModules(state, extracted, group)
    if (modules.isEmpty) {
      fail(s"Release group $group has no published modules yet, so there is nothing to release")
    }
    val mismatched = modules.filter(module => extracted.get(module / version) != expected)
    if (mismatched.nonEmpty) {
      fail(
        mismatched.map(module => s"${module.project} has version ${extracted.get(module / version)}").mkString(
          s"Release group $group must have version $expected, but ",
          ", ",
          ". Is the tag on this commit and the working tree clean?"
        )
      )
    }
    modules.flatMap(module => moduleChecks(group).map(check => s"${module.project}/$check"))
  }

  private def publishedModules(state: State, extracted: Extracted, group: String): List[ProjectRef] =
    extracted.structure.allProjectRefs.toList
      .filter(module => extracted.getOpt(module / releaseGroup).contains(group))
      .filterNot(module => extracted.runTask(module / publish / skip, state)._2)

  private def moduleChecks(group: String): List[String] = {
    val common = List("checkLicenses", "testFull", "mimaReportBinaryIssues")
    if (group == core || group == intellij) common else "checkReleasedCore" :: common
  }

  private def readState(base: File, separator: String, date: Date, group: String): GroupState = {
    val instance = DynVer(Some(base), separator, tagPrefix(group))
    GroupState(instance.getGitDescribeOutput(date), instance.getGitPreviousStableTag)
  }

  private def stateOf(states: Map[String, GroupState], group: String): GroupState =
    states.getOrElse(group, sys.error(s"Release group $group is not listed in ReleaseGroups.buildSettings in build.sbt"))

  private def onCore(dependency: ModuleID, coreNames: Set[String], coreVersion: String): ModuleID =
    if (coreNames.contains(dependency.name)) dependency.withRevision(coreVersion) else dependency

  private def verifyReleasedCore(
    module:          String,
    snapshot:        Boolean,
    released:        Option[String],
    base:            File,
    coreDirectories: Seq[File]
  ): Unit =
    if (!snapshot) {
      released.fold(fail(s"$module: release core first, there is no ${tagPrefix(core)}* tag before this commit")) {
        coreVersion => verifyUnchanged(module, coreVersion, base, coreDirectories)
      }
    }

  private def verifyUnchanged(module: String, coreVersion: String, base: File, coreDirectories: Seq[File]): Unit = {
    val paths   = coreDirectories.flatMap(directory => IO.relativize(base, directory)).toList
    val changed = git(base, ("diff" :: "--name-only" :: s"${tagPrefix(core)}$coreVersion" :: "HEAD" :: "--" :: paths)*)
      .getOrElse(fail(s"$module: cannot compare core with ${tagPrefix(core)}$coreVersion"))
    if (changed.nonEmpty) {
      fail(
        s"$module: core changed since ${tagPrefix(core)}$coreVersion, release core first. Changed files:\n$changed"
      )
    }
  }

  private def fail(message: String): Nothing =
    throw new MessageOnlyException(message)

  private def git(base: File, arguments: String*): Option[String] =
    Try(Process("git" +: arguments, base).!!.trim).toOption
}
