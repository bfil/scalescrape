package com.bfil.scalescrape.matchers

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpMethod, HttpRequest}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.stream.Materializer
import org.specs2.matcher.{Expectable, Matcher}

trait HttpRequestMatchers {
  def hasMethod(method: HttpMethod) = new Matcher[HttpRequest] {
    def apply[A <: HttpRequest](req: Expectable[A]) = result(method == req.value.method,
      s"HTTP request with unexpected method ${req.value.method}, expected $method",
      s"HTTP request with unexpected method ${req.value.method}, expected $method",
      req)
  }

  def hasUri(uri: String) = new Matcher[HttpRequest] {
    def apply[A <: HttpRequest](req: Expectable[A]) = result(uri == req.value.uri.toString,
      s"HTTP request with unexpected uri ${req.value.uri}, expected $uri",
      s"HTTP request with unexpected uri ${req.value.uri}, expected $uri",
      req)
  }

  def hasHeader(header: HttpHeader) = new Matcher[HttpRequest] {
    def apply[A <: HttpRequest](req: Expectable[A]) = result(req.value.headers.contains(header),
      s"HTTP request with unexpected headers ${req.value.headers}, expected $header",
      s"HTTP request with unexpected headers ${req.value.headers}, expected $header",
      req)
  }

  def hasCookies(cookies: List[HttpCookie]) = new Matcher[HttpRequest] {
    def apply[A <: HttpRequest](req: Expectable[A]) = result(cookies == req.value.cookies,
      s"HTTP request with unexpected cookies ${req.value.cookies}, expected $cookies",
      s"HTTP request with unexpected cookies ${req.value.cookies}, expected $cookies",
      req)
  }

  def hasData(data: String)(implicit mat: Materializer) = new Matcher[HttpRequest] {
    def apply[A <: HttpRequest](req: Expectable[A]) = result(data == req.value.entity.asString,
      s"HTTP request with unexpected content ${req.value.entity.asString}, expected $data",
      s"HTTP request with unexpected content ${req.value.entity.asString}, expected $data",
      req)
  }

  implicit class RichHttpEntity(entity: HttpEntity)(implicit mat: Materializer) {
    def asString() = Await.result(entity.toStrict(1 second), 1 second).data.utf8String
  }
}
