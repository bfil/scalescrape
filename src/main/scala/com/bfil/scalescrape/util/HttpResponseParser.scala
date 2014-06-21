package com.bfil.scalescrape.util

import org.json4s.string2JsonInput

import spray.client.pipelining.unmarshal
import spray.http.HttpResponse
import spray.httpx.unmarshalling.BasicUnmarshallers.StringUnmarshaller
import spray.httpx.unmarshalling.UnmarshallerLifting.{fromMessageUnmarshaller, fromResponseUnmarshaller}

object HttpResponseParser {
  implicit val stringUnmarshaller = fromResponseUnmarshaller(fromMessageUnmarshaller(StringUnmarshaller))
  
  def toHtml(httpResponse: HttpResponse) = org.jsoup.Jsoup.parse(unmarshal[String](stringUnmarshaller)(httpResponse))
  def toXml(httpResponse: HttpResponse) = scala.xml.XML.loadString(unmarshal[String](stringUnmarshaller)(httpResponse))
  def toJson(httpResponse: HttpResponse) = org.json4s.native.JsonMethods.parse(unmarshal[String](stringUnmarshaller)(httpResponse))
}