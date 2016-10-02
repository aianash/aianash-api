package actors.trail

import scala.concurrent.duration._

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import akka.pattern.pipe
import akka.util.Timeout

import aianonymous.commons.core.protocols._, Implicits._
// import aianash.commons.trail._


class TrailClient extends Actor with ActorLogging {

  def receive = {
    case _ =>
  }
}

object TrailClient {
  final val name = "trail-client"
  def props = Props(classOf[TrailClient])
}