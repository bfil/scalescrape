package io.bfil.scalescrape.util

import scala.concurrent.Future

import io.bfil.scalescrape.dsl.ScrapingDsl
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}

trait HttpResponseParser extends PredefinedFromEntityUnmarshallers {
  self: ScrapingDsl =>

  protected def unmarshal[T: FromEntityUnmarshaller]: HttpResponse => Future[T] = { response =>
    if (response.status.isSuccess) Unmarshal(response).to[T]
    else response.discardEntityBytes().future.map { s =>
      throw new UnsuccessfulResponseException(response)
    }
  }

  def parseAsHtml(httpResponse: HttpResponse) =
    unmarshal[String](stringUnmarshaller)(httpResponse) map { responseString =>
      org.jsoup.Jsoup.parse(responseString)
    }
  def parseAsXml(httpResponse: HttpResponse) =
    unmarshal[String](stringUnmarshaller)(httpResponse) map { responseString =>
      scala.xml.XML.loadString(responseString)
    }
  def parseAsJson(httpResponse: HttpResponse) =
    unmarshal[String](stringUnmarshaller)(httpResponse) map { responseString =>
      org.json4s.native.JsonMethods.parse(responseString)
    }
}

class UnsuccessfulResponseException(response: HttpResponse) extends Exception(s"Unsuccessful response: $response")
