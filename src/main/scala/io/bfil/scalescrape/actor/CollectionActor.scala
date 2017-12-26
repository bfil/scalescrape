package io.bfil.scalescrape.actor

import io.bfil.scalescrape.dsl.CollectionDsl

import akka.actor.{Actor, ActorLogging}

trait CollectionActor[T <: Actor] extends Actor with ActorLogging with CollectionDsl[T] {
  implicit val actorContext = context
  implicit val executionContext = context.dispatcher
}
