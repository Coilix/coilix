import sbt.*

object ApiSite {

  def assemble(scaladoc: File, javadoc: File, output: File): File = {
    IO.delete(output)
    IO.copyDirectory(scaladoc, output)
    IO.copyDirectory(javadoc, output / "javadoc")
    output
  }
}
