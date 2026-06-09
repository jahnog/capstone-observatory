lazy val root =
  project.in(file("."))
    .aggregate(LocalProject("capstoneUI"))

name := "global-warming-observatory"

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
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8", // image rendering for the map tiles
  "org.scalatest" %% "scalatest" % "3.0.9" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.6" % Test,
  "junit" % "junit" % "4.10" % Test
)

Test / parallelExecution := false

// Required for scrimage 2.1.8 on Java 9+: it reflects into sun.nio.ch internals
// when writing images.
fork := true
javaOptions ++= Seq(
  "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
)
