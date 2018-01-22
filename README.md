Scalescrape
===========

[![Codacy Badge](https://www.codacy.com/project/badge/364c7acbb81e47cc8f3e6712289dcb2f)](https://www.codacy.com/app/bfil/scalescrape)

A Scala web scraping library, based on [Scalext](https://github.com/bfil/scalext), for building Akka actor systems that scrape and collect data from any type of website.

Setting up the dependencies
---------------------------

__Scalescrape__ is available on `Maven Central` (since version `0.4.0`), and it is cross compiled and published for Scala 2.12 and 2.11.

*Older artifacts versions are not available anymore due to the shutdown of my self-hosted Nexus Repository in favour of Bintray*

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "io.bfil" %% "scalescrape" % "0.4.1"
)
```


If you have issues resolving the dependency, you can add the following resolver:

```scala
resolvers += Resolver.bintrayRepo("bfil", "maven")
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

The main components of __Scalescrape__ are the `ScrapingActor` and the `CollectionActor` traits.

To understand the details of the internal mechanics of the DSL read the documentation of [Scalext](https://github.com/bfil/scalext).

#### Scraping Actor

You can create a scraping Akka actor and use the scraping DSL by extending the `ScrapingActor` trait.

__scrape__

```scala
def scrape[T](scrapingAction: Action)(implicit ac: ActorContext): Unit
```

It creates a `ScrapingContext` with a reference to the current message sender and an empty cookie jar, and passes it to the inner action:

```scala
scape {
  ctx => println(ctx.requestor, ctx.cookies) // current sender, cookies
}
```

__get__

```scala
def get(url: String)(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse]
```

It sends a `GET` request to the url provided and passes the response into the inner action:

```scala
get("http://www.example.com/home") { response =>
  ctx => Unit
}
```

__post__

```scala
def post[T](request: Request[T])(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse]
```

It sends the `POST` request and passes the response into the inner action:

```scala
post(Request("http://www.example.com/update", "some data")) { response =>
  ctx => Unit
}
```

__postForm__

```scala
def postForm[T](form: Form)(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse]
```

It sends the `POST` request with form data and passes the response into the inner action:

```scala
postForm("http://www.example.com/submit-form", Map("some" -> "data")) { response =>
  ctx => Unit
}
```

__put__

```scala
def put[T](request: Request[T])(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse]
```

It sends the `PUT` request and passes the response into the inner action:

```scala
put(Request("http://www.example.com/update", "some data")) { response =>
  ctx => Unit
}
```

__delete__

```scala
def delete[T](request: Request[T])(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse]
```

It sends the `DELETE` request and passes the response into the inner action:

```scala
delete(Request("http://www.example.com/update", "some data")) { response =>
  ctx => Unit
}
```

__cookies__

```scala
def cookies: ChainableAction1[Map[String, HttpCookie]]
```

It extracts the cookies from the current contexts and passes them into the inner function:

```scala
cookies { cookies =>
  ctx => Unit
}
```

__withCookies__

```scala
def withCookies(cookies: Map[String, HttpCookie]): ChainableAction0
```

It replaces the cookies of the current contexts with the ones specified and calls the inner function with the new context:

```scala
withCookies(newCookies) {
  ctx => Unit
}
```

__addCookie__

```scala
def addCookie(cookie: HttpCookie): ChainableAction0
```

Adds a cookie to the current contexts and calls the inner function with the new context:

```scala
addCookie(newCookie) {
  ctx => Unit
}
```

__dropCookie__

```scala
def dropCookie(cookieName: String): ChainableAction0
```

Adds a cookie to the current contexts and calls the inner function with the new context:

```scala
dropCookie("someCookie") {
  ctx => Unit
}
```

__complete__

```scala
def complete[T](message: Any): ActionResult
```

Completes the scraping action by sending the specified message back to the original sender:

```scala
complete("done")
```

__fail__

```scala
def fail: ActionResult
```

Returns an Akka status failure message back to the original sender:

```scala
fail
```

#### Collection Actor

You can create a collection Akka actor and use the collection DSL by extending the `CollectionActor[T]` trait, where `T` is a `ScrapingActor`.

__collect__

```scala
def collect(collectionAction: Action)(implicit tag: ClassTag[Scraper], ac: ActorContext): Unit
```

It spawns an instance of the `ScarpingActor` specified as a type parameter under the hood. It creates a `CollectionContext` with a reference to the scraping actor and to the current message sender, and passes the context to the inner action:

```scala
collect {
  ctx => println(ctx.requestor, ctx.scraper) // current sender, scraping actor
}
```

__collectUsingScraper__

```scala
def collectUsingScraper(scraper: ActorRef)(collectionAction: Action)(implicit ac: ActorContext): Unit
```

It creates a `CollectionContext` with a reference to the scraping actor specified and to the current message sender, and passes the context to the inner action:

```scala
collectUsingScraper(myScrapingActor) {
  ctx => println(ctx.requestor, ctx.scraper) // current sender, scraping actor
}
```

__askTo__

```scala
def askTo(messages: Any)(implicit ec: ExecutionContext): ChainableAction1[Any]
```

It sends messages (using `akka.pattern.ask`) to the scraping actor in the collection context and passes the received messages to the inner action:

_Please note_: it currently handles correctly only up to 3 parameters.

```scala
askTo("say hello") {
  case "hello" => complete("thanks")
  case _ => fail
}

askTo("say hello", "say world") {
  case ("hello", "world") => complete("thanks")
  case _ => fail
}

askTo("say hello", "say world", "say bye") {
  case ("hello", "world", "bye") => complete("bye")
  case _ => fail
}
```

__scraper__

```scala
def scraper: ChainableAction1[ActorRef]
```

It extracts the scraper from the current contexts and passes them into the inner function:

```scala
scraper { scraper =>
  ctx => Unit
}
```

__withScraper__

```scala
def withScraper(scraper: ActorRef): ChainableAction0
```

It replaces the scraper of the current contexts with the ones specified and calls the inner function with the new context:

```scala
withScraper(newScraper) {
  ctx => Unit
}
```

__notify__

```scala
def notify[T](message: Any): ChainableAction0
```

Sends a message back to the original sender and calls the inner action:

```scala
notify("hello") {
  ctx => Unit
}
```

__complete__

```scala
def complete[T](message: Any): ActionResult
```

Completes the collection action by sending the specified message back to the original sender:

```scala
complete("done")
```

__keepAlive__

```scala
def keepAlive: ActionResult
```

Completes the collection action by not sending any message back to the original sender and keeping the scraping actor alive:

```scala
keepAlive
```

__fail__

```scala
def fail: ActionResult
```

Returns an Akka status failure message back to the original sender and kills the scraping actor:

```scala
fail
```

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2014-2017 Bruno Filippone <http://bfil.io>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
