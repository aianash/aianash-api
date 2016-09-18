package actors.analytics

import scala.concurrent.duration._

import javax.inject._

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.pipe
import akka.util.Timeout

import play.Configuration

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.services.{UUIDGenerator, NextId}

//
sealed trait IdGenerationServiceProtocol
case object GetAianId extends IdGenerationServiceProtocol with Replyable[Long]
case object GetSessionId extends IdGenerationServiceProtocol with Replyable[Long]

//
class IdGenerationService @Inject() (config: Configuration) extends Actor with ActorLogging {

  import context.dispatcher

  val serviceId    = config.getLong("service.id")
  val datacenterId = config.getLong("datacenter.id")

  val uuid = context.actorOf(UUIDGenerator.props(serviceId, datacenterId))
  context watch uuid

  def receive = {
    case GetAianId =>
      implicit val timeout = Timeout(2 seconds)
      (uuid ?= NextId("aian-id")) map (_.get) pipeTo sender()

    case GetSessionId =>
      implicit val timeout = Timeout(2 seconds)
      (uuid ?= NextId("session-id")) map (_.get) pipeTo sender()

    case _ =>
      log.error("Invalid message !")
  }

}

//
object IdGenerationService {

  final val name = "id-generation-service"

  def props = Props(classOf[IdGenerationService])

}