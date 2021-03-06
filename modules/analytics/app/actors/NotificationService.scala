package actors.analytics

import scala.collection.mutable.{ArrayBuffer, StringBuilder, Map => MMap}
import scala.concurrent.duration._
import scala.concurrent.Future

import java.net.URL
import java.lang.System

import akka.actor.{Actor, Props, ActorLogging}
import akka.pattern.pipe
import akka.routing.FromConfig
import akka.util.Timeout

import play.api.Logger

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL
import aianonymous.commons.events._

import cassie.core.protocols.customer.{GetPageId, GetWebPage}
import cassie.core.protocols.events._


sealed trait NotificationProtocol
case class Notify(tokenId: Long, aianId: Long, sessionId: Long, pageUrl: PageURL, encoded: String)
  extends NotificationProtocol
case class AddSession(aianId: Long, sessionId: Long, url: PageURL, timestamp: Long)
  extends NotificationProtocol with Replyable[Boolean]

class NotificationService extends Actor with ActorLogging  {

  import context.dispatcher

  val customerService = context.actorOf(FromConfig.props(), name = "customer-service")
  val eventService = context.actorOf(FromConfig.props(), name = "event-service")

  def receive = {
    case Notify(tokenId, aianId, sessionId, pageUrl, encoded) =>
      try {
        implicit val timeout = Timeout(2 seconds)
        (customerService ?= GetPageId(pageUrl)) foreach {
          case Some(pageId) =>
            val (startTmstr, data) = encoded splitAt 13
            val startTime = startTmstr.toLong
            val events = toEvents(startTime, lzwDecode(data))
            val pageEvents = PageEvents(sessionId, pageId, startTime, events)
            val eventsSession = EventsSession(tokenId, aianId, sessionId, Seq(pageEvents))
            eventService ! InsertEvents(eventsSession, 1)

          case None =>
        }
      } catch {
        case ex: UnsupportedOperationException =>
      }

    case AddSession(aianId, sessionId, url, timestamp) =>
      implicit val timeout = Timeout(2 seconds)
      (customerService ?= GetWebPage(url)) flatMap {
        case Some(pageUrl) =>
          implicit val timeout = Timeout(2 seconds)
          eventService ?= InsertSession(pageUrl.tokenId, pageUrl.pageId, timestamp, sessionId, aianId)

        case None =>
          Logger.warn(s"No page url entry for url [$url]")
          Future.failed(new Exception("No page url entry"))
      } pipeTo sender()
  }

  //
  def lzwDecode(encoded: String) = {
    val dict = MMap.empty[Int, String]
    val data = encoded.split("")
    var decoded = StringBuilder.newBuilder
    var currChar: Char = data(0).charAt(0)
    var prevphrase: String = data(0)
    decoded += currChar
    var phrase: String = ""
    var code = 256
    for(d <- data.drop(1)) {
      val currCode = d.charAt(0)
      if(currCode < 256) phrase = d
      else phrase = dict.getOrElse(currCode, prevphrase + currChar)
      decoded ++= phrase
      currChar = phrase.charAt(0)
      dict += code -> (prevphrase + currChar)
      code += 1
      prevphrase = phrase
    }
    decoded.result
  }

  //
  def toEvents(startTm: Long, decoded: String) = {
    var data = decoded.toArray
    var evParam  = ArrayBuffer.empty[Int]
    var numstr = StringBuilder.newBuilder
    var inevent = false
    var evtype: Char = '\u0000'
    val events = ArrayBuffer.empty[TrackingEvent]
    var pIdx = 0
    var tm = startTm

    for(d <- data) {
      d match {
        case 'f' | 'r' | 'p' | 's' =>
          if(!numstr.isEmpty) evParam += numstr.result().toInt
          if(!evParam.isEmpty) {
            var event: TrackingEvent =
              evtype match {
                case 'f' => crPageFragmentView(tm, evParam)
                case 'r' => crScanning(tm, evParam)
                case 'p' => crMousePath(tm, evParam)
                case 's' => crSectionView(tm, evParam)
                case _ =>
                  throw new UnsupportedOperationException("evtype is not a recognized event type")
              }
            events += event
          }
          numstr.clear()
          evParam.clear()
          evtype = d
          pIdx = 0
        case ',' =>
            pIdx += 1
            if(pIdx == 1) tm += numstr.result().toLong
            else evParam += numstr.result().toInt
            numstr.clear()
        case n => numstr += n
      }
    }

    events
  }

  private def crPageFragmentView(timestamp: Long, params: ArrayBuffer[Int]) =
    PageFragmentView(
      scrollPos    = Position(params(1), params(2)),
      windowHeight = params(3),
      windowWidth  = params(4),
      startTime    = timestamp,
      duration     = params(0))

  private def crScanning(timestamp: Long, params: ArrayBuffer[Int]) =
    Scanning(
      fromPos   = Position(params(1), params(2)),
      toPos     = Position(params(1) + params(3), params(2) + params(4)),
      startTime = timestamp,
      duration  = params(0))

  private def crMousePath(timestamp: Long, params: ArrayBuffer[Int]) = {
    val dur = params(0)
    val sections = ArrayBuffer.empty[(Int, Position)]
    var i = 1;
    var x = 0;
    var y = 0;
    while(i < params.length) {
      x += params(i + 1)
      y += params(i + 2)
      sections += params(i) -> Position(x, y)
      i += 3
    }
    MousePath(sections, timestamp, dur)
  }

  private def crSectionView(timestamp: Long, params: ArrayBuffer[Int]) =
    SectionView(
      sectionId = params(1),
      pos       = Position(params(2), params(3)),
      startTime = timestamp,
      duration  = params(0))
}

object NotificationService {
  final val name  = "notification-service"
  def props = Props(classOf[NotificationService])
}