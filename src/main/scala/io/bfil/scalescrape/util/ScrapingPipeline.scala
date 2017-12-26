package io.bfil.scalescrape.util

import scala.concurrent.Future

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import io.bfil.scalescrape.dsl.ScrapingDsl
import io.bfil.scalescrape.context.ScrapingContext
import io.bfil.scalescrape.data.ScrapingResponse

trait ScrapingPipeline extends RequestBuilding {
  self: ScrapingDsl =>

  protected val sendReceive: HttpRequest => Future[HttpResponse] = { req =>
    Http().singleRequest(req)
  }

  def addCookies(cookieJar: Map[String, HttpCookie]): HttpRequest => HttpRequest = { request =>
    val cookiePairs = cookieJar.values.map(_.pair).toList
    if(cookiePairs.isEmpty) request else request.withHeaders(Cookie(cookiePairs))
  }

  def storeCookies(context: ScrapingContext): HttpResponse => ScrapingResponse = { response =>
    val cookies = response.headers.collect { case c: `Set-Cookie` => c }.map(_.cookie)
    val newCookies = cookies.map { case cookie => (cookie.name, cookie) }.toMap
    ScrapingResponse(response, context.withCookies(context.cookies ++ newCookies))
  }

  def sendReceiveWithScrapingContext(context: ScrapingContext) =
    addCookies(context.cookies) ~> sendReceive ~> storeCookies(context)
}
