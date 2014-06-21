package com.bfil.scalescrape.actor

import akka.actor.{Actor, ActorLogging}

trait BaseActor extends Actor with ActorLogging {
  implicit def actorContext = context
  implicit def executionContext = context.dispatcher
}