package com.bfil.scalescrape.dsl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml.Elem

import org.json4s.JsonAST.JValue
import org.jsoup.nodes.Document

import com.bfil.scalext.ContextualDsl
import com.bfil.scalescrape.context.ScrapingContext
import com.bfil.scalescrape.data.{Form, Request, ScrapingRequest, ScrapingResponse}
import com.bfil.scalescrape.util.{HttpResponseParser, JsoupPimps, ScrapingPipeline}

import akka.actor.{ActorContext, ActorRef}
import shapeless.{:: => ::, HNil}
import spray.http.{FormData, HttpCookie, HttpMethods, HttpRequest, HttpResponse}
import spray.httpx.RequestBuilding.RequestBuilder
import spray.httpx.marshalling.Marshaller

trait ScrapingDsl extends ContextualDsl[ScrapingContext] with ScrapingPipeline with JsoupPimps {
  def scrape[T](scrapingAction: Action)(implicit ac: ActorContext) = {
    def sealedScrape(sender: ActorRef) = scrapingAction(ScrapingContext(sender))
    sealedScrape(ac.sender)
  }

  def get(magnet: GetRequestMagnet): ChainableAction1[HttpResponse] = magnet
  def post(magnet: PostRequestMagnet): ChainableAction1[HttpResponse] = magnet
  def postForm(magnet: PostFormRequestMagnet): ChainableAction1[HttpResponse] = magnet
  def put(magnet: PutRequestMagnet): ChainableAction1[HttpResponse] = magnet
  def delete(magnet: DeleteRequestMagnet): ChainableAction1[HttpResponse] = magnet

  def cookies: ChainableAction1[Map[String, HttpCookie]] = extract(_.cookies)
  def withCookies(cookies: Map[String, HttpCookie]): ChainableAction0 = mapContext(_.withCookies(cookies))
  def addCookie(cookie: HttpCookie): ChainableAction0 = mapContext(_.addCookie(cookie))
  def dropCookie(cookieName: String): ChainableAction0 = mapContext(_.dropCookie(cookieName))

  def complete[T](message: Any): ActionResult = ActionResult { _.complete(message) }
  def fail: ActionResult = ActionResult { _.fail }

  implicit class ParsableHttpResponse(httpResponse: HttpResponse) {
    def asHtml(f: Document => Action) = f(HttpResponseParser.toHtml(httpResponse))
    def asXml(f: Elem => Action) = f(HttpResponseParser.toXml(httpResponse))
    def asJson(f: JValue => Action) = f(HttpResponseParser.toJson(httpResponse))
  }

  private def sendReceive(scrapingRequest: ScrapingRequest)(implicit ec: ExecutionContext, ac: ActorContext): Future[ScrapingResponse] = {
    val pipeline = sendReceiveWithScrapingContext(scrapingRequest.context)
    pipeline { scrapingRequest.request }
  }

  private def sendScrapingRequest(httpRequest: HttpRequest)(implicit ec: ExecutionContext, ac: ActorContext): ChainableAction1[HttpResponse] =
    receiveScrapingResponse { ctx =>
      val scrapingRequest = ScrapingRequest(httpRequest, ctx)
      sendReceive(scrapingRequest)
    }

  private def receiveScrapingResponse(f: ScrapingContext => Future[ScrapingResponse])(implicit ec: ExecutionContext): ChainableAction1[HttpResponse] =
    new ChainableAction1[HttpResponse] {
      def happly(inner: (HttpResponse :: HNil) => Action) = ctx =>
        f(ctx).onComplete {
          case Success(res @ ScrapingResponse(response, newContext)) => inner(response :: HNil)(newContext)
          case Failure(ex) => throw ex
        }
    }

  trait GetRequestMagnet extends ChainableAction1[HttpResponse]

  object GetRequestMagnet {
    implicit def apply(url: String)(implicit ec: ExecutionContext, ac: ActorContext) = new GetRequestMagnet {
      def happly(inner: (HttpResponse :: HNil) => Action) =
        sendScrapingRequest(new RequestBuilder(HttpMethods.GET)(url)).happly(inner)
    }
  }

  trait PostFormRequestMagnet extends ChainableAction1[HttpResponse]

  object PostFormRequestMagnet {
    implicit def apply(form: Form)(implicit ec: ExecutionContext, ac: ActorContext) = new PostFormRequestMagnet {
      def happly(inner: (HttpResponse :: HNil) => Action) =
        sendScrapingRequest(new RequestBuilder(HttpMethods.POST)(form.action, FormData(form.data))).happly(inner)
    }
  }

  trait PostRequestMagnet extends ChainableAction1[HttpResponse]

  object PostRequestMagnet {
    implicit def apply[T](request: Request[T])(implicit m: Marshaller[T], ec: ExecutionContext, ac: ActorContext) = new PostRequestMagnet {
      def happly(inner: (HttpResponse :: HNil) => Action) =
        sendScrapingRequest(new RequestBuilder(HttpMethods.POST)(request.url, request.content)).happly(inner)
    }
  }

  trait PutRequestMagnet extends ChainableAction1[HttpResponse]

  object PutRequestMagnet {
    implicit def apply[T](request: Request[T])(implicit m: Marshaller[T], ec: ExecutionContext, ac: ActorContext) = new PutRequestMagnet {
      def happly(inner: (HttpResponse :: HNil) => Action) =
        sendScrapingRequest(new RequestBuilder(HttpMethods.PUT)(request.url, request.content)).happly(inner)
    }
  }

  trait DeleteRequestMagnet extends ChainableAction1[HttpResponse]

  object DeleteRequestMagnet {
    implicit def apply[T](request: Request[T])(implicit m: Marshaller[T], ec: ExecutionContext, ac: ActorContext) = new DeleteRequestMagnet {
      def happly(inner: (HttpResponse :: HNil) => Action) =
        sendScrapingRequest(new RequestBuilder(HttpMethods.DELETE)(request.url, request.content)).happly(inner)
    }
  }
}