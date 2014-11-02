resolvers += "BFil Nexus Snapshots (Private)" at "http://nexus.b-fil.com:8081/nexus/content/repositories/private-snapshots/"

credentials += Credentials(Path.userHome / ".ivy2" / ".bfil-credentials")

addSbtPlugin("com.bfil" % "sbt-bfil" % "0.1.0-SNAPSHOT")