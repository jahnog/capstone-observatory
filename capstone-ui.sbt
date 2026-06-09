lazy val capstoneUI =
  project.in(file("capstone-ui"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaVersion := "2.12.19",
      // Add the sources of the main project
      Compile / unmanagedSources ++= {
        val rootSourceDirectory = baseDirectory.value.getParentFile / "src" / "main" / "scala" / "observatory"
        Seq(
          rootSourceDirectory / "Interaction2.scala",
          rootSourceDirectory / "Signal.scala",
          rootSourceDirectory / "models.scala"
        )
      },
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.1.0",
        "com.lihaoyi" %%% "scalatags" % "0.8.5"
      ),
      scalaJSUseMainModuleInitializer := true
    )