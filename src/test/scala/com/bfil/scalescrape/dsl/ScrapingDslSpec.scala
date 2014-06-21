package com.bfil.scalescrape.dsl

import scala.concurrent.Future

import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JString
import org.json4s.jvalue2monadic

import com.bfil.scalescrape.context.ScrapingContext
import com.bfil.scalescrape.data.{Form, Request}

import spray.http.HttpCookie
import spray.http.HttpEntity.apply
import spray.http.HttpMethods.{DELETE, GET, POST, PUT}
import spray.http.HttpResponse
import spray.http.StatusCode.int2StatusCode
import spray.httpx.Json4sSupport

class ScrapingDslSpec extends DslSpec {

  case class Data(key: String, value: Int)

  "scrape" should {
    "call the inner action with a new scraping context" in new ScrapingDslSpecContext {
      scrape { ctx =>
        ctx.requestor must beEqualTo(testRequestor.ref)
      }
    }
  }

  "get" should {
    "send a GET request and call the inner action with the response" in new ScrapingDslSpecContext {
      responder.sendReceive(any) returns Future { HttpResponse(200, "hello") }
      asyncCheck(get("/some/url")) { response =>
        ctx => response.entity.asString must beEqualTo("hello")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(GET) and hasUri("/some/url") and hasData("")))
    }

    "send a GET request and parse an HTML response" in new ScrapingDslSpecContext {
      responder.sendReceive(any) returns Future { HttpResponse(200, "<span>hello</span>") }
      asyncCheck(get("/some/url")) { response =>
        response.asHtml { doc =>
          ctx => doc.$("span").text must beEqualTo("hello")
        }
      }
    }

    "send a GET request and parse an XML response" in new ScrapingDslSpecContext {
      responder.sendReceive(any) returns Future { HttpResponse(200, "<say>hello</say>") }
      asyncCheck(get("/some/url")) { response =>
        response.asXml { xml =>
          ctx => xml.text must beEqualTo("hello")
        }
      }
    }

    "send a GET request and parse a JSON response" in new ScrapingDslSpecContext {
      responder.sendReceive(any) returns Future { HttpResponse(200, "{\"say\":\"hello\"}") }
      asyncCheck(get("/some/url")) { response =>
        response.asJson { json =>
          ctx => (json \ "say") must beEqualTo(JString("hello"))
        }
      }
    }
  }

  "post" should {
    "send a POST request and call the inner action with the response" in new ScrapingDslSpecContext {
      val request = Request("/some/url", "some data")
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(post(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(POST) and hasUri("/some/url") and hasData("some data")))
    }

    "serialize a POST request to JSON and call the inner action with the response" in new ScrapingDslSpecContext with Json4sSupport {
      implicit def json4sFormats: Formats = DefaultFormats

      val request = Request("/some/url", Data("test", 1))
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(post(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(POST) and hasUri("/some/url") and hasData("{\"key\":\"test\",\"value\":1}")))
    }
  }

  "postForm" should {
    val formData = Map("key" -> "value")
    val form = Form("/some/url", formData)

    "send a POST request with form data and call the inner action with the response" in new ScrapingDslSpecContext {
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(postForm(form)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(POST) and hasUri("/some/url") and hasData("key=value")))
    }
  }

  "put" should {
    "send a PUT request and call the inner action with the response" in new ScrapingDslSpecContext {
      val request = Request("/some/url", "some data")
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(put(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(PUT) and hasUri("/some/url") and hasData("some data")))
    }

    "serialize a PUT request to JSON and call the inner action with the response" in new ScrapingDslSpecContext with Json4sSupport {
      implicit def json4sFormats: Formats = DefaultFormats

      val request = Request("/some/url", Data("test", 1))
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(put(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(PUT) and hasUri("/some/url") and hasData("{\"key\":\"test\",\"value\":1}")))
    }
  }

  "delete" should {
    "send a DELETE request and call the inner action with the response" in new ScrapingDslSpecContext {
      val request = Request("/some/url", "some data")
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(delete(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(DELETE) and hasUri("/some/url") and hasData("some data")))
    }

    "serialize a DELETE request to JSON and call the inner action with the response" in new ScrapingDslSpecContext with Json4sSupport {
      implicit def json4sFormats: Formats = DefaultFormats

      val request = Request("/some/url", Data("test", 1))
      responder.sendReceive(any) returns Future { HttpResponse(200, "done") }
      asyncCheck(delete(request)) { response =>
        ctx => response.entity.asString must beEqualTo("done")
      }
      there was one(responder).sendReceive(argThat(
        hasMethod(DELETE) and hasUri("/some/url") and hasData("{\"key\":\"test\",\"value\":1}")))
    }
  }

  "cookies" should {
    "extract the cookies from the context and pass them into the inner action" in new ScrapingDslSpecContext {
      implicit val scrapingContext = ScrapingContext(cookies = Map("authToken" -> HttpCookie("authToken", "someToken")))
      asyncCheck(cookies) { cookies =>
        ctx =>
          cookies.size must beEqualTo(1)
          cookies.get("authToken") must beEqualTo(Some(HttpCookie("authToken", "someToken")))
      }
    }

    "extract the cookies from the context even when there are none and pass them into the inner action" in new ScrapingDslSpecContext {
      implicit val scrapingContext = ScrapingContext()
      asyncCheck(cookies) { cookies =>
        ctx =>
          cookies.size must beEqualTo(0)
          cookies must beEqualTo(Map.empty)
      }
    }
  }

  "withCookies" should {
    "replace the cookies in the context and pass the new context into the inner action" in new ScrapingDslSpecContext {
      val newCookies = Map("authToken" -> HttpCookie("authToken", "someToken"))
      asyncCheck(withCookies(newCookies)) { ctx =>
        ctx.cookies.size must beEqualTo(1)
        ctx.cookies.get("authToken") must beEqualTo(Some(HttpCookie("authToken", "someToken")))
      }
    }
  }
  
  "addCookie" should {
    "add a cookie in the context and pass the new context into the inner action" in new ScrapingDslSpecContext {
      val newCookie = HttpCookie("authToken", "someToken")
      asyncCheck(addCookie(newCookie)) { ctx =>
        ctx.cookies.size must beEqualTo(1)
        ctx.cookies.get("authToken") must beEqualTo(Some(HttpCookie("authToken", "someToken")))
      }
    }
  }
  
  "dropCookie" should {
    "drop a cookie from the context and pass the new context into the inner action" in new ScrapingDslSpecContext {
      implicit val scrapingContext = ScrapingContext(cookies = Map("authToken" -> HttpCookie("authToken", "someToken")))
      asyncCheck(dropCookie("authToken")) { ctx =>
        ctx.cookies.size must beEqualTo(0)
        ctx.cookies must beEqualTo(Map.empty)
      }
    }
  }

  "complete" should {
    "send the scraping result to the requestor" in new ScrapingDslSpecContext {
      scrape {
        complete(TestProtocol.Done)
      }
      testRequestor.expectMsg(TestProtocol.Done)
    }
  }

  "fail" should {
    "send a failure status message to the requestor" in new ScrapingDslSpecContext {
      scrape {
        fail
      }
      testRequestor.expectMsg(akka.actor.Status.Failure)
    }
  }
}