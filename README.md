Scalescrape
===========

A Scala web scraping library, based on [Scalext](https://github.com/bfil/scalext), for building Akka actor systems that scrape and collect data from any website.

Setting up the dependencies
---------------------------

__Scalescrape__ is available at my [S3 Repository](http://shrub.appspot.com/bfil-mvn-repo), and it is cross compiled and published for both Scala 2.10 and 2.11.

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

The library offers to main actor traits that can be extended:

1. A `ScrapingActor`: which can be used to define the web scraping logic of an actor
2. A `CollectionActor`: which can be used to communicate to a `ScrapingActor` and collect all the data needed

### A basic example

The following example can be used to get some insight of how to use the library

#### The example website

The first step is to try to create a representation of the website that we are going to scrape, something like the following:

```scala
class ExampleWebsite {
  private val baseUrl = "http://www.example.com"
  
  val homePage = s"$baseUrl/home"
  
  def loginForm(username: String, password: String) =
    Form(s"$baseUrl/vm_sso/idp/login.action", Map(
      "username" -> username,
      "password" -> password))
      
  def updateAccountEmailRequest(newEmail: String) =
    Request(s"$baseUrl/account/update", s"""{"email": "$newEmail" }""")
}
```

The `ExampleWebsite` defines the url of the homepage, a login form and request object that can be used to update the account email on the example website.

`Form` and `Request` are part of the library, and are used to define forms or requests that you need to do in order to scrape the website.

#### The protocol

The following will be the message protocol used by the actors to communicate:

```scala
object ExampleProtocol {
  case class UpdateAccountEmailWithCredentials(username: String, password: String, newEmail: String)
  case class Login(username: String, password: String)
  case object LoggedIn
  case object LoginFailed
  case class UpdateAccountEmail(newEmail: String)
  case object EmailUpdated
  case object EmailUpToDate
}
```

#### The scraping actor

An example scraping actor can be defined like this:

```scala
class ExampleScraper extends ScrapingActor {
  // actor logic
}
```

In our actor logic we are going to create an instance of our `ExampleWebsite` for later use, we also create a variable to store some session cookies:

```scala
val website = new ExampleWebsite
var savedCookies: Map[String, HttpCookie] = Map.empty
```

In order to do anything on the website we have to login first, so let's define a method on the actor that logs a user in using his credentials:

```scala
private def login(username: String, password: String) =
  scrape { // (1)
    postForm(website.loginForm(username, password)) { response => // (2)
      response.asHtml { doc => // (3)
        doc.$("title").text match { // (4)
          case "Login error" => complete(LoginFailed) // (5)
          case _ =>
            cookies { cookies => // (6)
              savedCookies = cookies // (7)
              complete(LoggedIn) // (8)
            }
        }
      }
    }
  }
```

1. Uses the `scrape` method to initialize the scraping action
2. Posts the login form using the `postForm` method, passing the `Form` instance
3. Parse the response as HTML and provides a `JSoup` document
4. Uses `JSoup` to get the text of the title tag
5. If the title tag is "Login error" it completes by sending back `LoginFailed`
6. Otherwise it gets the cookies from the current session
7. Stores the session cookies in our actor variable
8. Completes and sends back `LoggedIn`

Please note that the `ScrapingActor` retains the cookies automatically between requests that are part of the same action (between `scrape` and `complete`), the cookies can be manipulated using the actions `addCookie`, `dropCookie`, and `withCookies`.

After logging in we can used the session cookies to perform other actions as authenticated users, let's create a method to update our email address on the website

```scala
private def updateAccountEmail(newEmail: String) =
  scrape { // (1)
    withCookies(savedCookies) { // (2)
      get(website.homePage) { response => // (3)
        response.asHtml { doc => // (4)
          val currentEmail = doc.$("#account-email").text // (5)
          if (currentEmail != newEmail) { // (6)
            post(website.updateAccountEmailRequest(newEmail)) { response => // (7)
              response.asJson { jsonResponse => // (8)
                (jsonResponse \ "error") match {
                  case JString(message) => fail // (9)
                  case _                => complete(EmailUpdated) // (10)
                }
              }
            }
          } else complete(EmailUpToDate) // (11)
        }
      }
    }
  }
```

1. Uses the `scrape` method to initialize the scraping action
2. Adds the session cookies we saved previously to the scraping context so that they will be sent with the following requests
3. Gets the homepage of the example website
4. Parses the response as HTML
5. Gets the value of the current account email from the `JSoup` document
6. If the current email is different from the one we want to set
7. Posts a JSON request to the website to update our email
8. Parses the response as JSON and checks if there is an error message
9. Fails if the update email response contains an error message
10. Completes and sends back `EmailUpdated` if the email update was successful
11. If the current email is the same as the one we want to set we complete and send back `EmailUpToDate`

Finally, we can define our actor's `receive` method:

```scala
def receive = {
  case Login(username, password)    => login(username, password)
  case UpdateAccountEmail(newEmail) => updateAccountEmail(newEmail)
}
```

This actor can now be used to login to our example website and update our email address by sending the appropriate messages to it.

Let's continue and create an actor that performs both actions for us.

#### The collection actor

An example collection actor can be defined like this:

```scala
class ExampleCollector extends CollectionActor[ExampleScraper] {
  // actor logic
}
```

Here's the actor logic:

```scala
def receive = {
  case UpdateAccountEmailWithCredentials(username, password, newEmail) =>
    collect { // (1)
      askTo(Login(username, password)) { // (2)
        case LoggedIn => // (3)
          askTo(UpdateAccountEmail(newEmail)) { // (4)
            case x => complete(x) // (5)
          }
        case LoginFailed => complete(LoginFailed) // (6)
      }
    }
}
```

1. Uses the `collect` method to initialize the collection action by creating an `ExampleScraper` actor under the hood
2. Asks the scraper to login with the credentials received
3. If the scraper returns `LoggedIn`
4. It goes on by asking it to `UpdateAccountEmail` with the new email
5. Then it completes and sends back whatever is received by the scraper as the response of the action (the complete action kills the internal scraping actor)
6. In case the login fails it sends back `LoginFailed`

This was a simple example of some of the capabilities of the library, for more details use the documentation.

### Documentation

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
