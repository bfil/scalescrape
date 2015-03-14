import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild with BFilPlugins {

  val buildVersion = "0.2.0-SNAPSHOT"
    
  lazy val project = BFilProject("scalescrape", file("."))
  .settings(libraryDependencies ++= Dependencies.all(scalaVersion.value))
}

object Dependencies {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.2"

  def all(scalaVersion: String) = Seq(
    "com.bfil" %% "scalext" % "0.2.0-SNAPSHOT",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    scalaVersion match {
      case "2.11.6" => "com.chuusai" %% "shapeless" % "2.0.0"
      case "2.10.5" => "com.chuusai" %% "shapeless" % "1.2.4"
    },
    "org.jsoup" % "jsoup" % "1.7.2",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    scalaVersion match {
      case "2.11.6" => "io.spray" %% "spray-testkit" % sprayVersion % "test" exclude("org.specs2", "specs2_2.11")
      case "2.10.5" => "io.spray" %% "spray-testkit" % sprayVersion % "test" exclude("org.specs2", "specs2_2.10")
    },
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.json4s" %% "json4s-ext" % "3.2.10",
    "org.specs2" %% "specs2-core" % "2.4.17" % "test",
    "org.specs2" %% "specs2-mock" % "2.4.17" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test")
}