import sbt.*

object LicenseAudit {
  private val allowed: List[String] = List(
    "apache",
    "mit",
    "bsd",
    "eclipse public license - v 2.0",
    "eclipse public license v2.0",
    "epl-2.0",
    "mozilla public license, version 2.0",
    "mpl-2.0"
  )

  private val forbidden: List[String] = List("gpl", "affero", "commons clause", "business source")

  private val chosenLicenses: Map[String, String] = Map(
    "org.reflections:reflections" -> "Apache-2.0 (dual-licensed with WTFPL)",
    "org.javassist:javassist"     -> "Apache-2.0 (tri-licensed with MPL-1.1 and LGPL-2.1)"
  )

  private val auditedConfigurations: Set[String] = Set("compile", "runtime")

  def check(report: UpdateReport, ownOrganization: String, projectId: String): Unit =
    violations(report, ownOrganization) match {
      case Nil => ()
      case found =>
        sys.error(
          s"Dependencies of $projectId with licenses outside the allowed list (docs/legal/dependency-licenses.md):\n${found.mkString("\n")}"
        )
    }

  private def violations(report: UpdateReport, ownOrganization: String): List[String] =
    report.configurations.toList
      .filter(configuration => auditedConfigurations.contains(configuration.configuration.name))
      .flatMap(_.modules)
      .filterNot(_.evicted)
      .filterNot(_.module.organization == ownOrganization)
      .distinctBy(coordinates)
      .filterNot(isAllowed)
      .map(describe)

  private def coordinates(module: ModuleReport): String =
    s"${module.module.organization}:${module.module.name.stripSuffix("_3").stripSuffix("_2.13")}"

  private def isAllowed(module: ModuleReport): Boolean =
    chosenLicenses.contains(coordinates(module)) || hasAllowedLicense(module)

  private def hasAllowedLicense(module: ModuleReport): Boolean =
    module.licenses.nonEmpty && module.licenses.forall { case (name, _) => isAllowedName(name.toLowerCase) }

  private def isAllowedName(name: String): Boolean =
    !forbidden.exists(name.contains) && allowed.exists(name.contains)

  private def describe(module: ModuleReport): String = {
    val licenses = module.licenses.map(_._1).mkString(", ")
    s"  ${coordinates(module)}:${module.module.revision} -> ${if (licenses.isEmpty) "no license in POM" else licenses}"
  }
}
