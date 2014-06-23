Scalescrape
===========

A Scala web scraping library to create Akka actor systems that scrape and collect data from any website.

Set up the dependencies
-----------------------

Scalescrape is available at my [S3 Repository](http://shrub.appspot.com/bfil-mvn-repo), and it is cross compiled and published for both Scala 2.10 and 2.11.

Using SBT, add the following plugin:

```scala
addSbtPlugin("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.2.0")
```

Add the following dependency to your SBT build file:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "scalescrape" % "0.1.0"
)
```

Don't forget to add the following resolver:

```scala
resolvers += "BFil S3 Repo Releases" at "s3://bfil-mvn-repo.s3-eu-west-1.amazonaws.com/releases"
```

### Using snapshots

If you need a snapshot dependency:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "scalescrape" % "0.2.0-SNAPSHOT"
)

resolvers += "BFil S3 Repo Snapshots" at "s3://bfil-mvn-repo.s3-eu-west-1.amazonaws.com/snapshots"
```

Usage
-----

TODO

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2014 Bruno Filippone <http://b-fil.com>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.