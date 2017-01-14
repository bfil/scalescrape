package com.bfil.scalescrape.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.{ActorMaterializer, Materializer}
import com.bfil.scalescrape.dsl.ScrapingDsl

trait ScrapingActor extends Actor with ActorLogging with ScrapingDsl {
  implicit val actorContext = context
  implicit val actorSystem = context.system
  implicit val executionContext = context.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()(actorSystem)
}
