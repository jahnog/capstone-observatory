package observatory

import org.scalatest.FunSuite

trait ProcessPriorityTest extends FunSuite {

  private val appliedEnvVar = "TILE_GENERATION_LOW_PRIORITY_APPLIED"
  private val niceLevelEnvVar = "TILE_GENERATION_NICE_LEVEL"
  private val ioniceClassEnvVar = "TILE_GENERATION_IONICE_CLASS"
  private val ioniceLevelEnvVar = "TILE_GENERATION_IONICE_LEVEL"
  private val maxThreadsEnvVar = "TILE_GENERATION_MAX_THREADS"

  test("shouldReexec only on Linux when the low-priority marker is absent") {
    assert(ProcessPriority.shouldReexec(Map.empty))
    assert(!ProcessPriority.shouldReexec(Map(appliedEnvVar -> "1")))
    assert(ProcessPriority.shouldApplyInProcess("Linux"))
    assert(!ProcessPriority.shouldApplyInProcess("Mac OS X"))
  }

  test("lowPriorityCommand wraps java with ionice, nice, and active processor limits") {
    val command = ProcessPriority.lowPriorityCommand(
      Seq("java", "-jar", "globalwarming.jar"),
      Map(
        "PATH" -> "/usr/bin",
        niceLevelEnvVar -> "19",
        ioniceClassEnvVar -> "2",
        ioniceLevelEnvVar -> "7",
        maxThreadsEnvVar -> "2"
      )
    )

    assert(command.take(8) == Seq("ionice", "-c", "2", "-n", "7", "nice", "-n", "19"))
    assert(command.contains("-XX:ActiveProcessorCount=2"))
    assert(command.contains("globalwarming.jar"))
  }

  test("lowPriorityCommand uses idle ionice class without a level flag") {
    val command = ProcessPriority.lowPriorityCommand(
      Seq("java"),
      Map(
        "PATH" -> "/usr/bin",
        ioniceClassEnvVar -> "3"
      )
    )

    assert(command.take(4) == Seq("ionice", "-c", "3", "nice"))
  }

  test("applyToCurrentProcessCommand targets the current pid with renice and ionice") {
    val commands = ProcessPriority.applyToCurrentProcessCommand(
      "4242",
      Map(
        niceLevelEnvVar -> "15",
        ioniceClassEnvVar -> "3"
      )
    )

    assert(commands == Seq(
      Seq("renice", "-n", "15", "-p", "4242"),
      Seq("ionice", "-c", "3", "-p", "4242")
    ))
  }

  test("maxThreads defaults to half of the available processors with a minimum of one") {
    assert(ProcessPriority.maxThreads(Map.empty, 8) == 4)
    assert(ProcessPriority.maxThreads(Map.empty, 1) == 1)
    assert(ProcessPriority.maxThreads(Map(maxThreadsEnvVar -> "3"), 8) == 3)
  }
}