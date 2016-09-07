package actors.customer

import scala.concurrent.duration._

import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import akka.pattern.pipe
import akka.util.Timeout

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.customer._

import cassie.core.protocols.customer._


sealed trait CustomerConfiguratorProtocol
case class AddPageTags(tags: Seq[PageTags]) extends CustomerConfiguratorProtocol with Replyable[Boolean]
case class GetPageTags(tid: Long, pid: Long) extends CustomerConfiguratorProtocol with Replyable[Seq[PageTags]]
case class GetTokenId(domain: String) extends CustomerConfiguratorProtocol with Replyable[Option[Domain]]
case class GetPageId(url: URL, tokenId: Long) extends CustomerConfiguratorProtocol with Replyable[Long]


class CustomerConfigurator extends Actor with ActorLogging {

  import context.dispatcher

  private val customerService = context.actorOf(FromConfig.props(), name = "customer-service")

  def receive = {
    case AddPageTags(tags) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= InsertPageTags(tags)) pipeTo sender()

    case GetPageTags(tid, pid) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= FetchPageTags(tid, pid)) pipeTo sender()

    case GetTokenId(domain) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetDomain(domain)) pipeTo sender()

    case GetPageId(url, tokenId) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetOrCreatePageId(url, tokenId)) pipeTo sender()
  }

}

object CustomerConfigurator {

  final val name = "customer-configurator"
  def props = Props(classOf[CustomerConfigurator])

}