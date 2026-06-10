package observatory

import java.lang.ProcessBuilder.Redirect

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.immutable.ParRange
import scala.util.Try

import java.util.concurrent.ForkJoinPool

object ProcessPriority {

  private val AppliedEnvVar = "TILE_GENERATION_LOW_PRIORITY_APPLIED"
  private val NiceLevelEnvVar = "TILE_GENERATION_NICE_LEVEL"
  private val IoniceClassEnvVar = "TILE_GENERATION_IONICE_CLASS"
  private val IoniceLevelEnvVar = "TILE_GENERATION_IONICE_LEVEL"
  private val MaxThreadsEnvVar = "TILE_GENERATION_MAX_THREADS"
  private val TileDelayMsEnvVar = "TILE_GENERATION_TILE_DELAY_MS"

  @volatile private var taskSupport: ForkJoinTaskSupport = _

  def ensureLowPriority(): Unit = {
    if (!isLinux(sys.props.getOrElse("os.name", ""))) {
      return
    }

    if (shouldReexec(sys.env)) {
      reexecUnderLowPriority()
    }

    applyToCurrentProcess()
    configureParallelism()
    logRuntimeProfile()
  }

  def limitedParRange(range: Range): ParRange = {
    val parallel = range.par
    Option(taskSupport).foreach(parallel.tasksupport = _)
    parallel
  }

  def yieldBetweenTiles(): Unit = {
    Thread.`yield`()
    configuredTileDelayMs().foreach(Thread.sleep)
  }

  private[observatory] def shouldReexec(environment: Map[String, String]): Boolean =
    !environment.contains(AppliedEnvVar)

  private[observatory] def shouldApplyInProcess(osName: String): Boolean =
    isLinux(osName)

  private[observatory] def isLinux(osName: String): Boolean =
    osName.toLowerCase.contains("linux")

  private[observatory] def niceLevel(environment: Map[String, String]): String =
    environment.getOrElse(NiceLevelEnvVar, "15")

  private[observatory] def ioniceClass(environment: Map[String, String]): String =
    environment.getOrElse(IoniceClassEnvVar, "3")

  private[observatory] def ioniceLevel(environment: Map[String, String]): String =
    environment.getOrElse(IoniceLevelEnvVar, "7")

  private[observatory] def maxThreads(environment: Map[String, String], availableProcessors: Int): Int =
    environment
      .get(MaxThreadsEnvVar)
      .flatMap(value => Try(value.toInt).toOption)
      .filter(_ > 0)
      .getOrElse(math.max(1, availableProcessors / 2))

  private[observatory] def configuredTileDelayMs(environment: Map[String, String] = sys.env): Option[Long] =
    environment
      .get(TileDelayMsEnvVar)
      .flatMap(value => Try(value.toLong).toOption)
      .filter(_ > 0)

  private[observatory] def lowPriorityCommand(command: Seq[String], environment: Map[String, String] = sys.env): Seq[String] = {
    val nice = niceLevel(environment)
    val ioniceClassValue = ioniceClass(environment)
    val ioniceLevelValue = ioniceLevel(environment)
    val threads = maxThreads(environment, Runtime.getRuntime.availableProcessors())

    require(command.nonEmpty, "Current process command line is empty.")

    if (!isCommandAvailable("nice")) {
      throw new IllegalStateException("nice is required to run tile generation at low priority.")
    }

    if (!isCommandAvailable("ionice")) {
      throw new IllegalStateException("ionice is required to run tile generation at low priority.")
    }

    if (!isCommandAvailable("renice")) {
      throw new IllegalStateException("renice is required to run tile generation at low priority.")
    }

    val ionicePrefix =
      if (ioniceClassValue == "3") {
        Seq("ionice", "-c", ioniceClassValue)
      } else {
        Seq("ionice", "-c", ioniceClassValue, "-n", ioniceLevelValue)
      }

    ionicePrefix ++ Seq("nice", "-n", nice) ++ augmentJvmCommand(command, threads)
  }

  private[observatory] def augmentJvmCommand(command: Seq[String], maxThreadCount: Int): Seq[String] = {
    val activeProcessorFlag = s"-XX:ActiveProcessorCount=$maxThreadCount"
    if (command.contains(activeProcessorFlag)) {
      command
    } else {
      val (javaBinary, arguments) = command.splitAt(1)
      javaBinary ++ Seq(activeProcessorFlag) ++ arguments
    }
  }

  private[observatory] def applyToCurrentProcessCommand(pid: String, environment: Map[String, String]): Seq[Seq[String]] = {
    val nice = niceLevel(environment)
    val ioniceClassValue = ioniceClass(environment)
    val ioniceLevelValue = ioniceLevel(environment)

    val ioniceCommand =
      if (ioniceClassValue == "3") {
        Seq("ionice", "-c", ioniceClassValue, "-p", pid)
      } else {
        Seq("ionice", "-c", ioniceClassValue, "-n", ioniceLevelValue, "-p", pid)
      }

    Seq(
      Seq("renice", "-n", nice, "-p", pid),
      ioniceCommand
    )
  }

  private def reexecUnderLowPriority(): Unit = {
    val command = currentProcessCommand()
    val processBuilder = new ProcessBuilder(lowPriorityCommand(command).asJava)
    processBuilder.directory(new java.io.File(sys.props("user.dir")))
    processBuilder.inheritIO()
    processBuilder.environment().put(AppliedEnvVar, "1")

    println("Re-launching tile generation under ionice and nice.")

    val exitCode = processBuilder.start().waitFor()
    sys.exit(exitCode)
  }

  private def applyToCurrentProcess(): Unit = {
    val pid = ProcessHandle.current().pid().toString

    applyToCurrentProcessCommand(pid, sys.env).foreach(runCommand)
  }

  private def configureParallelism(): Unit = {
    val threads = maxThreads(sys.env, Runtime.getRuntime.availableProcessors())
    val pool = new ForkJoinPool(threads)
    taskSupport = new ForkJoinTaskSupport(pool)
  }

  private def logRuntimeProfile(): Unit = {
    val threads = maxThreads(sys.env, Runtime.getRuntime.availableProcessors())
    val delay = configuredTileDelayMs().map(ms => s"${ms}ms").getOrElse("disabled")
    println(
      s"Tile generation throttling: nice=${niceLevel(sys.env)}, ioniceClass=${ioniceClass(sys.env)}, " +
        s"maxThreads=$threads, tileDelay=$delay, activeProcessors=${Runtime.getRuntime.availableProcessors()}"
    )
  }

  private def currentProcessCommand(): Seq[String] = {
    val processInfo = ProcessHandle.current().info()
    val command = processInfo.command().orElseThrow(() => new IllegalStateException("Unable to determine the current Java command."))
    val arguments = processInfo.arguments().orElse(Array.empty[String])
    command +: arguments.toSeq
  }

  private def runCommand(command: Seq[String]): Unit = {
    val builder = new ProcessBuilder(command.asJava)
    builder.redirectOutput(Redirect.DISCARD)
    builder.redirectError(Redirect.DISCARD)

    val exitCode = builder.start().waitFor()
    if (exitCode != 0) {
      throw new IllegalStateException(s"Command failed (${command.mkString(" ")}): exit code $exitCode")
    }
  }

  private def isCommandAvailable(command: String): Boolean = {
    val builder = new ProcessBuilder("which", command)
    builder.redirectOutput(Redirect.DISCARD)
    builder.redirectError(Redirect.DISCARD)
    builder.start().waitFor() == 0
  }
}