import StudentBuild._

lazy val root =
  project.in(file("."))
    .aggregate(LocalProject("capstoneUI"))
    .settings(
      submitSetting,
      submitLocalSetting,
      commonSourcePackages := Seq(),
      styleCheckSetting,
      libraryDependencies += scalaTestDependency
    )
    .settings(packageSubmissionFiles: _*)

name := course.value ++ "-" ++ assignment.value

scalaVersion := "2.12.19"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xexperimental"
)

libraryDependencies ++= Seq(
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8", // for visualization
  // You don’t *have to* use Spark, but in case you want to, we have added the dependency
  "org.apache.spark" %% "spark-sql" % "2.4.8",
  // You don’t *have to* use akka-stream, but in case you want to, we have added the dependency
  "com.typesafe.akka" %% "akka-stream" % "2.4.12",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.12" % Test,
  // You don’t *have to* use Monix, but in case you want to, we have added the dependency
  "io.monix" %% "monix" % "2.1.1",
  // You don’t *have to* use fs2, but in case you want to, we have added the dependency
  "co.fs2" %% "fs2-io" % "0.9.2",
  "org.scalacheck" %% "scalacheck" % "1.12.6" % Test,
  "junit" % "junit" % "4.10" % Test
)

// courseId := "PCO2sYdDEeW0iQ6RUMSWEQ"

Test / parallelExecution := false // So that tests are executed for each milestone, one after the other

// Required for scrimage 2.1.8 (used by Visualization/Interaction) on Java 9+.
// scrimage uses reflection into sun.nio.ch internals for image output.
fork := true
javaOptions ++= Seq(
  "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
)
