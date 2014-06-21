package com.bfil.scalescrape.dsl

import akka.testkit.TestProbe

class CollectionDslSpec extends DslSpec {

  "collect" should {
    "call the inner action with a new collection context" in new CollectionDslSpecContext {
      collect { ctx =>
        ctx.requestor must beEqualTo(testRequestor.ref)
      }
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
  }
  
  "collectUsingScraper" should {
    "call the inner action with a new collection context using the scraper specified" in new CollectionDslSpecContext {
      val customScraper = TestProbe()
      collectUsingScraper(customScraper.ref) { ctx =>
        ctx.requestor must beEqualTo(testRequestor.ref)
        ctx.scraper must beEqualTo(customScraper.ref)
        ctx.scraper must not(beEqualTo(testScraper.ref))
      }
      testRequestor.ref must not(beTerminated)
      customScraper.ref must not(beTerminated)
    }
  }
  
  "askTo" should {
    "sends a message to the scraper and call the inner action with the scraper response" in new CollectionDslSpecContext {
      val checkResult = futureCheck(askTo("say hello")) { message =>
        ctx =>
          message must beEqualTo("hello")
          ctx.requestor must beEqualTo(testRequestor.ref)
          ctx.scraper must beEqualTo(testScraper.ref)
      }
      testScraper.expectMsg("say hello")
      testScraper.reply("hello")
      checkResult.wait
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
    
    "sends 2 messages to the scraper and call the inner action with the scraper response" in new CollectionDslSpecContext {
      val checkResult = futureCheck(askTo("say hello", "say world")) { case (message1, message2) =>
        ctx =>
          message1 must beEqualTo("hello")
          message2 must beEqualTo("world")
          ctx.requestor must beEqualTo(testRequestor.ref)
          ctx.scraper must beEqualTo(testScraper.ref)
      }
      testScraper.expectMsg("say hello")
      testScraper.reply("hello")
      testScraper.expectMsg("say world")
      testScraper.reply("world")
      checkResult.wait
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
    
    "sends 3 messages to the scraper and call the inner action with the scraper response" in new CollectionDslSpecContext {
      val checkResult = futureCheck(askTo("say hello", "say world", "say something")) { case (message1, message2, message3) =>
        ctx =>
          message1 must beEqualTo("hello")
          message2 must beEqualTo("world")
          message3 must beEqualTo("something")
          ctx.requestor must beEqualTo(testRequestor.ref)
          ctx.scraper must beEqualTo(testScraper.ref)
      }
      testScraper.expectMsg("say hello")
      testScraper.reply("hello")
      testScraper.expectMsg("say world")
      testScraper.reply("world")
      testScraper.expectMsg("say something")
      testScraper.reply("something")
      checkResult.wait
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
  }
  
  "scraper" should {
    "extract the scraper from the context and pass them into the inner action" in new CollectionDslSpecContext {
      asyncCheck(scraper) { scraper =>
        ctx =>
          scraper must beEqualTo(testScraper.ref)
      }
    }
  }
  
  "withScraper" should {
    "replace the cookies in the context and pass the new context into the inner action" in new CollectionDslSpecContext {
      val newScraper = TestProbe()
      asyncCheck(withScraper(newScraper.ref)) { ctx =>
        ctx.scraper must beEqualTo(newScraper.ref)
        ctx.scraper must not(beEqualTo(testScraper.ref))
      }
    }
  }
  
  "notify" should {
    "send the collection result to the testRequestor and keep the scraper alive" in new CollectionDslSpecContext {
      collect {
        notify(TestProtocol.Done) {
          ctx => Unit
        }
      }
      testRequestor.expectMsg(TestProtocol.Done)
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
  }

  "complete" should {
    "send the collection result to the testRequestor and kill the scraper" in new CollectionDslSpecContext {
      collect {
        complete(TestProtocol.Done)
      }
      testRequestor.expectMsg(TestProtocol.Done)
      testWatcher.expectTerminated(testScraper.ref)
      testRequestor.ref must not(beTerminated)
      testScraper.ref must beTerminated
    }
  }
  
  "keepAlive" should {
    "not send any result to the testRequestor and keep the scraper alive" in new CollectionDslSpecContext {
      collect {
        keepAlive
      }
      testRequestor.expectNoMsg
      testRequestor.ref must not(beTerminated)
      testScraper.ref must not(beTerminated)
    }
  }
  
  "fail" should {
    "send a failure to the testRequestor and kill the scraper" in new CollectionDslSpecContext {
      collect {
        fail
      }
      testRequestor.expectMsg(akka.actor.Status.Failure)
      testRequestor.ref must not(beTerminated)
      testScraper.ref must beTerminated
    }
  }
}