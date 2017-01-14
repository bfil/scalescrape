package com.bfil.scalescrape.actor

import com.bfil.scalescrape.dsl.CollectionDsl

import akka.actor.{Actor, ActorLogging}

trait CollectionActor[T <: Actor] extends Actor with ActorLogging with CollectionDsl[T] {
  implicit val actorContext = context
  implicit val executionContext = context.dispatcher
}
