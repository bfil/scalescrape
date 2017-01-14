import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild {

  lazy val project = BFilProject("scalescrape", file("."))
    .settings(crossScalaVersions := Seq("2.12.1", "2.11.8"))
    .settings(libraryDependencies ++= Dependencies.all)
}

object Dependencies {
  val akkaVersion = "2.4.16"
  val akkaHttpVersion = "10.0.1"
  val json4sVersion = "3.5.0"
  val scalextVersion = "0.3.0"
  val specs2Version = "3.8.6"

  val all = Seq(
    "com.bfil" %% "scalext" % scalextVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-json4s" % "1.11.0",
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.json4s" %% "json4s-ext" % json4sVersion,
    "org.jsoup" % "jsoup" % "1.10.2",
    "com.bfil" %% "scalext-testkit" % scalextVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "org.specs2" %% "specs2-core" % specs2Version % "test",
    "org.specs2" %% "specs2-mock" % specs2Version % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
  )
}
