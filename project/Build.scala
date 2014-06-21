import sbt._
import Keys._

object ScalescrapeBuild extends Build {

  val appVersion = "0.1.0-SNAPSHOT"

  lazy val Scalescrape = Project(
    id = "scalescrape",
    base = file("."),
    settings = Defaults.defaultSettings ++
      buildSettings ++
      compilersSettings ++
      Publish.settings)

  lazy val buildSettings = Seq(
    name := "scalescrape",
    organization := "com.bfil",
    version := appVersion,
    scalaVersion := "2.11.1",
    crossScalaVersions  := Seq("2.11.1", "2.10.4"),
    crossVersion := CrossVersion.binary,
    organizationName := "Bruno Filippone",
    organizationHomepage := Some(url("http://www.b-fil.com")),
    libraryDependencies <++= scalaVersion(Dependencies.all(_)),
    resolvers ++= Resolvers.all)

  lazy val compilersSettings = Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"))
}

object Dependencies {
  val akkaVersion = "2.3.3"
  val sprayVersion = "1.3.1"

  def all(scalaVersion: String) = Seq(
    "com.bfil" %% "scalext" % "0.1.0-SNAPSHOT",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    scalaVersion match {
      case "2.11.1" => "io.spray" %% "spray-can" % s"$sprayVersion-20140423"
      case "2.10.4" => "io.spray" % "spray-can" % sprayVersion
    },
    scalaVersion match {
      case "2.11.1" => "io.spray" %% "spray-routing" % s"$sprayVersion-20140423"
      case "2.10.4" => "io.spray" % "spray-routing" % sprayVersion
    },
    scalaVersion match {
      case "2.11.1" => "io.spray" %% "spray-client" % s"$sprayVersion-20140423"
      case "2.10.4" => "io.spray" % "spray-client" % sprayVersion
    },
    scalaVersion match {
      case "2.11.1" => "io.spray" %% "spray-testkit" % s"$sprayVersion-20140423" % "test"
      case "2.10.4" => "io.spray" % "spray-testkit" % sprayVersion % "test"
    },
    scalaVersion match {
      case "2.11.1" => "com.chuusai" %% "shapeless" % "2.0.0"
      case "2.10.4" => "com.chuusai" %% "shapeless" % "1.2.4"
    },
    "org.jsoup" % "jsoup" % "1.7.2",
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.json4s" %% "json4s-ext" % "3.2.10",
    "org.specs2" %% "specs2" % "2.3.12" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3"
)
}

object Resolvers {
  val all = Seq(
    "BFil S3 Repo Snapshots" at "s3://bfil-mvn-repo.s3-eu-west-1.amazonaws.com/snapshots",
    "Spray" at "http://repo.spray.io/",
    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/")
}

object Publish {
  def repository: Project.Initialize[Option[sbt.Resolver]] = version { (version: String) =>
    val s3Bucket = "s3://bfil-mvn-repo.s3-eu-west-1.amazonaws.com/"
    if (version.trim.endsWith("SNAPSHOT")) Some("BFil S3 Repo Snapshots" at s3Bucket + "snapshots")
    else Some("BFil S3 Repo Releases" at s3Bucket + "releases")
  }

  lazy val settings = Seq(
    publishMavenStyle := true,
    publishTo <<= repository,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("http://www.b-fil.com")),
    pomExtra := (
      <scm>
        <url>git://github.com/bfil/scalescrape.git</url>
        <connection>scm:git://github.com/bfil/scalescrape.git</connection>
      </scm>
      <developers>
        <developer>
          <id>bfil</id>
          <name>Bruno Filippone</name>
          <url>http://www.b-fil.com</url>
        </developer>
      </developers>))
}