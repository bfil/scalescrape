package io.bfil.scalescrape.dsl

import org.specs2.execute.AsResult

import io.bfil.scalext.testkit.ContextualDslTestkit
import io.bfil.scalescrape.actor.ScrapingActor
import io.bfil.scalescrape.context.CollectionContext

import akka.testkit.TestProbe

class TestScraper extends ScrapingActor {
  def receive = { case x => Unit }
}

trait CollectionDslSpecContext extends DslSpecContext with CollectionDsl[TestScraper] with ContextualDslTestkit[CollectionContext] {
  val testRequestor, testScraper, testWatcher = TestProbe()
  def around[T: AsResult](t: => T) = {
    actorContext.sender returns testRequestor.ref
    actorContext.actorOf(any) returns testScraper.ref
    testWatcher watch testRequestor.ref
    testWatcher watch testScraper.ref
    AsResult(t)
  }
  
  implicit val initialContext = CollectionContext(testRequestor.ref).withScraper(testScraper.ref)
}