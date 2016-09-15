package actors.behavior

import scala.concurrent.duration._

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import akka.pattern.pipe
import akka.util.Timeout

import aianonymous.commons.core.protocols._, Implicits._
import aianash.commons.behavior._


class BehaviorClient extends Actor with ActorLogging {

  def receive = {
    case _ =>
  }
}

object BehaviorClient {
  final val name = "behavior-client"
  def props = Props(classOf[BehaviorClient])
}