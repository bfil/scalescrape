package com.bfil.scalescrape.dsl

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.testkit.TestProbe
import com.bfil.scalext.testkit.ContextualDslTestkit
import com.bfil.scalescrape.context.ScrapingContext
import com.bfil.scalescrape.matchers.HttpRequestMatchers
import org.specs2.execute.AsResult

trait ScrapingDslSpecContext extends DslSpecContext with ScrapingDsl with ContextualDslTestkit[ScrapingContext] with HttpRequestMatchers {

  val testRequestor, testWatcher = TestProbe()
  val responder = mock[Responder]

  class Responder {
    def sendReceive(req: HttpRequest): Future[HttpResponse] = Future.successful(HttpResponse())
  }

  override def sendReceiveWithScrapingContext(context: ScrapingContext) =
    addCookies(context.cookies) ~> responder.sendReceive ~> storeCookies(context)

  def around[T: AsResult](t: => T) = {
    actorContext.sender returns testRequestor.ref
    testWatcher watch testRequestor.ref
    AsResult(t)
  }

  implicit val initialContext = ScrapingContext(testRequestor.ref).withCookies(Map.empty)
}
