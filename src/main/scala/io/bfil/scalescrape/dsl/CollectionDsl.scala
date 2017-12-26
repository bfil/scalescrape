package io.bfil.scalescrape.dsl

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import io.bfil.scalescrape.context.CollectionContext
import io.bfil.scalext.ContextualDsl

trait CollectionDsl[T <: Actor] extends ContextualDsl[CollectionContext] {
  type Scraper = T

  implicit def actorContext: ActorContext
  implicit def executionContext: ExecutionContext

  implicit val timeout: Timeout = 10 seconds

  def collect(collectionAction: Action)(implicit tag: ClassTag[Scraper]) = {
    val scraper = actorContext.actorOf(Props[Scraper])
    def sealedScrape(sender: ActorRef) = collectionAction(CollectionContext(sender).withScraper(scraper))
    sealedScrape(actorContext.sender)
  }
  def collectUsingScraper(scraper: ActorRef)(collectionAction: Action) = {
    def sealedScrape(sender: ActorRef) = collectionAction(CollectionContext(sender).withScraper(scraper))
    sealedScrape(actorContext.sender)
  }

  def askTo(magnet: AskMagnet): ChainableAction1[Any] = magnet.action

  def scraper: ChainableAction1[ActorRef] = extract(_.scraper)
  def withScraper(scraper: ActorRef): ChainableAction0 = mapContext(_.withScraper(scraper))

  def notify[T](message: Any): ChainableAction0 = mapContext { ctx => ctx.notify(message); ctx }
  def complete[T](message: Any): ActionResult = ActionResult { _.complete(message) }
  def keepAlive: ActionResult = ActionResult { ctx => Unit }
  def fail: ActionResult = ActionResult { _.fail }

  trait AskMagnet {
    val action: ChainableAction1[Any]
  }

  object AskMagnet {
    implicit def apply(messages: Any) = new AskMagnet {
      val action: ChainableAction1[Any] = ChainableAction { inner =>
        onSuccess { ctx: Context =>
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
        }.tapply(inner)
      }
    }
  }
}
