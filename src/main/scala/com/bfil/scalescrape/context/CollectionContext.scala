package com.bfil.scalescrape.context

import akka.actor.{ActorRef, PoisonPill, actorRef2Scala}

case class CollectionContext(requestor: ActorRef = ActorRef.noSender, scraper: ActorRef = ActorRef.noSender) {
  def withScraper(newScraper: ActorRef) = this.copy(scraper = newScraper)
  def notify(message: Any) = requestor ! message
  def complete(message: Any) = {
    scraper ! PoisonPill
    requestor ! message
  }
  def fail = {
    scraper ! PoisonPill
    requestor ! akka.actor.Status.Failure
  }
}