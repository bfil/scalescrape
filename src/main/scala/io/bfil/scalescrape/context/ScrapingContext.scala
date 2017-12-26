package io.bfil.scalescrape.context

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.HttpCookie

case class ScrapingContext(requestor: ActorRef = ActorRef.noSender, cookies: Map[String, HttpCookie] = Map.empty) {
  def withCookies(cookies: Map[String, HttpCookie]) = this.copy(cookies = cookies)
  def addCookie(cookie: HttpCookie) = this.copy(cookies = cookies + (cookie.name -> cookie))
  def dropCookie(cookieName: String) = this.copy(cookies = cookies - cookieName)
  def complete(message: Any) = requestor ! message
  def fail = requestor ! akka.actor.Status.Failure
}
