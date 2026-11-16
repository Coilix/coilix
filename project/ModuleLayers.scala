import sbt._
import sbt.internal.BuildDependencies

/** Checks the module layers from docs/architecture/modularity.md against the project dependencies of the build. */
object ModuleLayers {

  def check(dependencies: BuildDependencies, core: Seq[String], features: Seq[String]): Unit = {
    val coreViolations    = core.flatMap(id => reached(dependencies, id).intersect(features.toSet).map(feature => s"$id -> $feature"))
    val featureViolations =
      features.flatMap(id => reached(dependencies, id).intersect(features.toSet - id).map(other => s"$id -> $other"))
    val violations        = coreViolations ++ featureViolations
    if (violations.nonEmpty) {
      sys.error(
        "Module layers are broken (docs/architecture/modularity.md): the core and boot must not depend on feature modules, " +
          s"and feature modules must not depend on each other: ${violations.sorted.mkString(", ")}"
      )
    }
  }

  private def reached(dependencies: BuildDependencies, id: String): Set[String] = {
    def direct(project: String):                        Set[String] =
      dependencies.classpath.collect { case (ref, deps) if ref.project == project => deps.map(_.project.project) }.flatten.toSet
    def loop(pending: List[String], seen: Set[String]): Set[String] =
      pending match {
        case Nil          => seen
        case next :: rest =>
          val found = direct(next) -- seen
          loop(rest ++ found, seen ++ found)
      }
    loop(List(id), Set.empty)
  }
}
