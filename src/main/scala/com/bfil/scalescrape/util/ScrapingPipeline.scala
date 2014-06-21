package com.bfil.scalescrape.util

import scala.concurrent.ExecutionContext

import com.bfil.scalescrape.context.ScrapingContext
import com.bfil.scalescrape.data.ScrapingResponse

import akka.actor.ActorContext
import spray.client.pipelining.{WithTransformerConcatenation, sendReceive, sendReceive$default$3}
import spray.http.{HttpCookie, HttpHeaders, HttpRequest, HttpResponse}

trait ScrapingPipeline {
  def addCookies(cookieJar: Map[String, HttpCookie]): HttpRequest => HttpRequest = { request =>
    request.withHeaders(HttpHeaders.Cookie(cookieJar.values.toList))
  }
  
  def storeCookies(context: ScrapingContext): HttpResponse => ScrapingResponse = { response =>
    val cookies = response.headers.collect { case c: HttpHeaders.`Set-Cookie` => c }.map(_.cookie)
    val newCookies = cookies.map { case cookie => (cookie.name, cookie) }.toMap
    ScrapingResponse(response, context.withCookies(context.cookies ++ newCookies))
  }
  
  def sendReceiveWithScrapingContext(context: ScrapingContext)(implicit ec: ExecutionContext, ac: ActorContext) =
    addCookies(context.cookies) ~> sendReceive ~> storeCookies(context)
}