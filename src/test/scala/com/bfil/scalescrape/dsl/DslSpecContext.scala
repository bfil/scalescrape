package com.bfil.scalescrape.dsl

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.specs2.matcher.{Expectable, Matcher, ResultMatchers, ThrownExpectations}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgThat
import org.specs2.mutable.Around

import akka.actor.{ActorContext, ActorRef, ActorSystem}
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout

trait DslSpecContext extends Around with Mockito with ArgThat with ThrownExpectations with ResultMatchers {
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val actorSystem = ActorSystem("test-system")
  implicit val actorContext = mock[ActorContext]

  implicit val selectionTimeout: Timeout = 3 seconds

  private def isTerminated(ref: ActorRef) = try {
    Await.result(actorSystem.actorSelection(ref.path).resolveOne, 3 seconds); false
  } catch { case _: Throwable => true }

  def beTerminated = new Matcher[ActorRef] {
    def apply[A <: ActorRef](actor: Expectable[A]) = result(isTerminated(actor.value),
      s"$actor was not terminated",
      s"$actor was terminated unexpectedly",
      actor)
  }
}