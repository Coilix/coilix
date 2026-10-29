import sbt.*

object IntellijPlugin {
  val pluginFileName: String = "coilix-slf4s.jar"

  val ideaHome: File =
    file(sys.env.getOrElse("IDEA_HOME", "C:/Program Files/JetBrains/IntelliJ IDEA 262.4852.50"))

  val ideaConfig: File =
    file(sys.env.getOrElse("IDEA_CONFIG", s"${sys.props("user.home")}/AppData/Roaming/JetBrains/IntelliJIdea2026.2"))

  private val scalaPlugin: File = ideaConfig / "plugins" / "Scala"

  def isAvailable: Boolean =
    (ideaHome / "lib").isDirectory && (scalaPlugin / "lib").isDirectory

  def compileJars: Seq[File] =
    jarsIn(ideaHome / "lib") ++ jarsIn(ideaHome / "plugins" / "java" / "lib") ++ jarsIn(scalaPlugin / "lib")

  def install(pluginJar: File): File = {
    val destination = ideaConfig / "plugins" / pluginFileName
    IO.copyFile(pluginJar, destination)
    destination
  }

  private def jarsIn(directory: File): Seq[File] =
    Option(directory.listFiles()).toSeq.flatten.filter(_.getName.endsWith(".jar"))
}
