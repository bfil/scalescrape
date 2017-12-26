package io.bfil.scalescrape.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.{ActorMaterializer, Materializer}
import io.bfil.scalescrape.dsl.ScrapingDsl

trait ScrapingActor extends Actor with ActorLogging with ScrapingDsl {
  implicit val actorContext = context
  implicit val actorSystem = context.system
  implicit val executionContext = context.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()(actorSystem)
}
