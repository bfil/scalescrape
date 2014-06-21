package com.bfil.scalescrape.actor

import com.bfil.scalescrape.dsl.CollectionDsl

import akka.actor.Actor

trait CollectionActor[T <: Actor] extends BaseActor with CollectionDsl[T]