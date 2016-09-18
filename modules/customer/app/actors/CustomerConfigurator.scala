package actors.customer

import scala.concurrent.duration._

import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import akka.pattern.pipe
import akka.util.Timeout

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL
import aianonymous.commons.customer._

import cassie.core.protocols.customer._


sealed trait CustomerConfiguratorProtocol
case class AddPageTags(tags: Seq[PageTags]) extends CustomerConfiguratorProtocol with Replyable[Boolean]
case class ObtainPageTags(tid: Long, pid: Long) extends CustomerConfiguratorProtocol with Replyable[Seq[PageTags]]
case class ObtainTokenId(domain: String) extends CustomerConfiguratorProtocol with Replyable[Option[Domain]]
case class ObtainPageId(url: PageURL) extends CustomerConfiguratorProtocol with Replyable[Option[Long]]
case class ObtainOrCreatePageId(url: PageURL, tokenId: Long, name: String) extends CustomerConfiguratorProtocol with Replyable[Long]


class CustomerConfigurator extends Actor with ActorLogging {

  import context.dispatcher

  private val customerService = context.actorOf(FromConfig.props(), name = "customer-service")

  def receive = {
    case AddPageTags(tags) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= InsertPageTags(tags)) pipeTo sender()

    case ObtainPageTags(tid, pid) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetPageTags(tid, pid)) pipeTo sender()

    case ObtainTokenId(domain) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetDomain(domain)) pipeTo sender()

    case ObtainPageId(url) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetPageId(url)) pipeTo sender()

    case ObtainOrCreatePageId(url, tokenId, name) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetOrCreatePageId(url, tokenId, name)) pipeTo sender()
  }

}

object CustomerConfigurator {

  final val name = "customer-configurator"
  def props = Props(classOf[CustomerConfigurator])

}