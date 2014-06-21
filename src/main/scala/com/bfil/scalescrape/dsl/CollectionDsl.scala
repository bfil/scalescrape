package com.bfil.scalescrape.dsl

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag

import com.bfil.scalext.ContextualDsl
import com.bfil.scalescrape.context.CollectionContext

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import shapeless.{:: => ::, HNil}

trait CollectionDsl[T <: Actor] extends ContextualDsl[CollectionContext] {
  type Scraper = T

  implicit val timeout: Timeout = 10 seconds

  def collect(collectionAction: Action)(implicit tag: ClassTag[Scraper], ac: ActorContext) = {
    val scraper = ac.actorOf(Props[Scraper])
    def sealedScrape(sender: ActorRef) = collectionAction(CollectionContext(sender).withScraper(scraper))
    sealedScrape(ac.sender)
  }
  def collectUsingScraper(scraper: ActorRef)(collectionAction: Action)(implicit ac: ActorContext) = {
    def sealedScrape(sender: ActorRef) = collectionAction(CollectionContext(sender).withScraper(scraper))
    sealedScrape(ac.sender)
  }

  def askTo(magnet: AskMagnet): ChainableAction1[Any] = magnet

  def scraper: ChainableAction1[ActorRef] = extract(_.scraper)
  def withScraper(scraper: ActorRef): ChainableAction0 = mapContext(_.withScraper(scraper))

  def notify[T](message: Any): ChainableAction0 = mapContext { ctx => ctx.notify(message); ctx }
  def complete[T](message: Any): ActionResult = ActionResult { _.complete(message) }
  def keepAlive: ActionResult = ActionResult { ctx => Unit }
  def fail: ActionResult = ActionResult { _.fail }

  trait AskMagnet extends ChainableAction1[Any]

  object AskMagnet {
    implicit def apply(messages: Any)(implicit ec: ExecutionContext) = new AskMagnet {
      def happly(inner: ((Any) :: HNil) => Action) = onSuccess { ctx: Context =>
        messages match {
          case (m1, m2, m3) => Future.sequence(List(ctx.scraper ? m1, ctx.scraper ? m2, ctx.scraper ? m3)) map {
            case List(res1, res2, res3) => (res1, res2, res3)
          }
          case (m1, m2) => Future.sequence(List(ctx.scraper ? m1, ctx.scraper ? m2)) map {
            case List(res1, res2) => (res1, res2)
          }
          case m1 => Future.sequence(List(ctx.scraper ? m1)) map {
            case List(res1) => (res1)
          }
        }

      }.happly(inner)
    }
  }
}