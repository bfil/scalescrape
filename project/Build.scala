import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild {
    
  lazy val project = BFilProject("scalescrape", file("."))
  .settings(libraryDependencies ++= Dependencies.all(scalaVersion.value))
}

object Dependencies {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.2"

  def all(scalaVersion: String) = Seq(
    "com.bfil" %% "scalext" % "0.2.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "org.jsoup" % "jsoup" % "1.8.1",
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.json4s" %% "json4s-ext" % "3.2.11",
    "com.bfil" %% "scalext-testkit" % "0.2.0" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    scalaVersion match {
      case "2.11.7" => "io.spray" %% "spray-testkit" % sprayVersion % "test" exclude("org.specs2", "specs2_2.11")
      case "2.10.5" => "io.spray" %% "spray-testkit" % sprayVersion % "test" exclude("org.specs2", "specs2_2.10")
    },
    "org.specs2" %% "specs2-core" % "2.4.17" % "test",
    "org.specs2" %% "specs2-mock" % "2.4.17" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test")
}