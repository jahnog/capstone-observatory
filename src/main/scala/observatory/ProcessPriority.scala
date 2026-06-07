package observatory

import java.lang.ProcessBuilder.Redirect

import scala.collection.JavaConverters._

object ProcessPriority {

  private val AppliedEnvVar = "TILE_GENERATION_LOW_PRIORITY_APPLIED"
  private val NiceLevelEnvVar = "TILE_GENERATION_NICE_LEVEL"
  private val IoniceClassEnvVar = "TILE_GENERATION_IONICE_CLASS"

  def ensureLowPriority(): Unit = {
    if (shouldEnforce(sys.props.getOrElse("os.name", ""), sys.env)) {
      val command = currentProcessCommand()
      val processBuilder = new ProcessBuilder(lowPriorityCommand(command).asJava)
      processBuilder.directory(new java.io.File(sys.props("user.dir")))
      processBuilder.inheritIO()
      processBuilder.environment().put(AppliedEnvVar, "1")

      println("Re-launching tile generation under ionice and nice.")

      val exitCode = processBuilder.start().waitFor()
      sys.exit(exitCode)
    }
  }

  private[observatory] def shouldEnforce(osName: String, environment: Map[String, String]): Boolean =
    osName.toLowerCase.contains("linux") && !environment.contains(AppliedEnvVar)

  private[observatory] def lowPriorityCommand(command: Seq[String], environment: Map[String, String] = sys.env): Seq[String] = {
    val niceLevel = environment.getOrElse(NiceLevelEnvVar, "15")
    val ioniceClass = environment.getOrElse(IoniceClassEnvVar, "3")

    require(command.nonEmpty, "Current process command line is empty.")

    if (!isCommandAvailable("nice")) {
      throw new IllegalStateException("nice is required to run tile generation at low priority.")
    }

    if (!isCommandAvailable("ionice")) {
      throw new IllegalStateException("ionice is required to run tile generation at low priority.")
    }

    Seq("ionice", "-c", ioniceClass, "nice", "-n", niceLevel) ++ command
  }

  private def currentProcessCommand(): Seq[String] = {
    val processInfo = ProcessHandle.current().info()
    val command = processInfo.command().orElseThrow(() => new IllegalStateException("Unable to determine the current Java command."))
    val arguments = processInfo.arguments().orElse(Array.empty[String])
    command +: arguments.toSeq
  }

  private def isCommandAvailable(command: String): Boolean = {
    val builder = new ProcessBuilder("which", command)
    builder.redirectOutput(Redirect.DISCARD)
    builder.redirectError(Redirect.DISCARD)
    builder.start().waitFor() == 0
  }
}