package io.bfil.scalescrape.dsl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml.Elem

import akka.actor.{ActorContext, ActorRef, ActorSystem}
import akka.http.scaladsl.client.RequestBuilding.RequestBuilder
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{FormData, HttpMethod, HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.stream.Materializer
import io.bfil.scalescrape.context.ScrapingContext
import io.bfil.scalescrape.data.{Form, Request, ScrapingRequest, ScrapingResponse}
import io.bfil.scalescrape.util.{HttpResponseParser, JsoupPimps, ScrapingPipeline}
import io.bfil.scalext.ContextualDsl
import org.json4s.JsonAST.JValue
import org.jsoup.nodes.Document

trait ScrapingDsl extends ContextualDsl[ScrapingContext] with ScrapingPipeline
  with HttpResponseParser with JsoupPimps {

  implicit def actorContext: ActorContext
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer

  def scrape[T](scrapingAction: Action) = {
    def sealedScrape(sender: ActorRef) = scrapingAction(ScrapingContext(sender))
    sealedScrape(actorContext.sender)
  }

  def get(magnet: RequestMagnet): ChainableAction1[HttpResponse] = magnet(HttpMethods.GET)
  def post(magnet: RequestMagnet): ChainableAction1[HttpResponse] = magnet(HttpMethods.POST)
  def postForm(magnet: RequestMagnet): ChainableAction1[HttpResponse] = magnet(HttpMethods.POST)
  def put(magnet: RequestMagnet): ChainableAction1[HttpResponse] = magnet(HttpMethods.PUT)
  def delete(magnet: RequestMagnet): ChainableAction1[HttpResponse] = magnet(HttpMethods.DELETE)

  def cookies: ChainableAction1[Map[String, HttpCookie]] = extract(_.cookies)
  def withCookies(cookies: Map[String, HttpCookie]): ChainableAction0 = mapContext(_.withCookies(cookies))
  def addCookie(cookie: HttpCookie): ChainableAction0 = mapContext(_.addCookie(cookie))
  def dropCookie(cookieName: String): ChainableAction0 = mapContext(_.dropCookie(cookieName))

  def complete[T](message: Any): ActionResult = ActionResult { _.complete(message) }
  def fail: ActionResult = ActionResult { _.fail }

  implicit class ParsableHttpResponse(httpResponse: HttpResponse) {
    def asHtml(f: Document => Action) = onSuccess(parseAsHtml(httpResponse))(f)
    def asXml(f: Elem => Action) = onSuccess(parseAsXml(httpResponse))(f)
    def asJson(f: JValue => Action) = onSuccess(parseAsJson(httpResponse))(f)
  }

  private def sendReceive(scrapingRequest: ScrapingRequest): Future[ScrapingResponse] = {
    val pipeline = sendReceiveWithScrapingContext(scrapingRequest.context)
    pipeline { scrapingRequest.request }
  }

  private def sendScrapingRequest(httpRequest: HttpRequest): ChainableAction1[HttpResponse] =
    receiveScrapingResponse { ctx =>
      val scrapingRequest = ScrapingRequest(httpRequest, ctx)
      sendReceive(scrapingRequest)
    }

  private def receiveScrapingResponse(f: ScrapingContext => Future[ScrapingResponse]): ChainableAction1[HttpResponse] =
    ChainableAction { inner =>
      ctx =>
        f(ctx).onComplete {
          case Success(res @ ScrapingResponse(response, newContext)) => inner(Tuple1(response))(newContext)
          case Failure(ex) => throw ex
        }
    }

  trait RequestMagnet {
    def apply(method: HttpMethod): ChainableAction1[HttpResponse]
  }

  object RequestMagnet {
    implicit def fromURL(url: String) = new RequestMagnet {
      def apply(method: HttpMethod): ChainableAction1[HttpResponse] = ChainableAction { inner =>
        sendScrapingRequest(new RequestBuilder(method)(url)).tapply(inner)
      }
    }
    implicit def fromForm(form: Form) = new RequestMagnet {
      def apply(method: HttpMethod): ChainableAction1[HttpResponse] = ChainableAction { inner =>
        sendScrapingRequest(new RequestBuilder(method)(form.action, FormData(form.data))).tapply(inner)
      }
    }
    implicit def fromRequest[T: ToEntityMarshaller](request: Request[T]) =
      new RequestMagnet {
        def apply(method: HttpMethod): ChainableAction1[HttpResponse] = ChainableAction { inner =>
          sendScrapingRequest(new RequestBuilder(method)(request.url, request.content)).tapply(inner)
        }
      }
  }
}
