import sbt._

trait CommonBuild {

  val course = SettingKey[String]("course")

  val assignment = SettingKey[String]("assignment")

  val courseId = SettingKey[String]("courseId")

  val commonSourcePackages = SettingKey[Seq[String]]("commonSourcePackages")

  lazy val scalaTestDependency = "org.scalatest" %% "scalatest" % "3.0.9"

}
