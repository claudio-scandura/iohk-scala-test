val settings = {
  name := "iohk-scala-test"
  version := "1.0"

  scalaVersion := "2.11.8"
}



val akkaVersion = "2.5.4"

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe" % "config" % "1.3.0",
  "org.bouncycastle" % "bcprov-jdk16" % "1.45",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test,it",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(libraryDependencies ++= dependencies)
  .settings(settings ++ Defaults.itSettings: _*)
