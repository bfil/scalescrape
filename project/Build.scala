import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild with BFilPlugins {

  val buildVersion = "0.2.0-SNAPSHOT"
    
  lazy val project = BFilProject("scalescrape", file("."))
  .settings(libraryDependencies ++= Dependencies.all(scalaVersion.value))
}

object Dependencies {
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.3.2"

  def all(scalaVersion: String) = Seq(
    "com.bfil" %% "scalext" % "0.1.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    scalaVersion match {
      case "2.11.4" => "com.chuusai" %% "shapeless" % "2.0.0"
      case "2.10.4" => "com.chuusai" %% "shapeless" % "1.2.4"
    },
    "org.jsoup" % "jsoup" % "1.7.2",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %% "spray-testkit" % sprayVersion,
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.json4s" %% "json4s-ext" % "3.2.10",
    "org.specs2" %% "specs2" % "2.3.12" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test")
}