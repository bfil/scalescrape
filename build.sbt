lazy val project = Project("scalescrape", file("."))
  .settings(settings, libraryDependencies ++= Seq(
    "io.bfil" %% "scalext" % scalextVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-json4s" % "1.19.0",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.json4s" %% "json4s-ext" % json4sVersion,
    "org.jsoup" % "jsoup" % "1.10.2",
    "io.bfil" %% "scalext-testkit" % scalextVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "org.specs2" %% "specs2-core" % specs2Version % "test",
    "org.specs2" %% "specs2-mock" % specs2Version % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
  ))

lazy val settings = Seq(
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.12.4", "2.11.12"),
  organization := "io.bfil",
  organizationName := "Bruno Filippone",
  organizationHomepage := Some(url("http://bfil.io")),
  homepage := Some(url("https://github.com/bfil/scalescrape")),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  developers := List(
    Developer("bfil", "Bruno Filippone", "bruno@bfil.io", url("http://bfil.io"))
  ),
  startYear := Some(2014),
  publishTo := Some("Bintray" at s"https://api.bintray.com/maven/bfil/maven/${name.value}"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".bintray-credentials"),
  scmInfo := Some(ScmInfo(
    url(s"https://github.com/bfil/scalescrape"),
    s"git@github.com:bfil/scalescrape.git"
  ))
)

lazy val akkaVersion = "2.5.8"
lazy val akkaHttpVersion = "10.0.11"
lazy val json4sVersion = "3.5.3"
lazy val scalextVersion = "0.4.0"
lazy val specs2Version = "3.8.6"