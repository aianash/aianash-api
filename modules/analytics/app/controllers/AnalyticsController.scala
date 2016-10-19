package controllers.analytics

import scala.concurrent.duration._
import scala.concurrent.Future

import java.net.URL
import javax.inject._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.Configuration
import views.html._

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL

import actors.analytics.{NotificationService, Notify, AddToCSV, AddSession}
import actors.analytics.{IdGenerationService, GetAianId, GetSessionId}

//
@Singleton
class AnalyticsController @Inject() (system: ActorSystem, config: Configuration,
    @Named(NotificationService.name) notification: ActorRef,
    @Named(IdGenerationService.name) idGenerationService: ActorRef
  ) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def aianashjs = Action { implicit request =>
    Ok(views.js.aianash(config.getString("analytics.analytics-url"), config.getString("analytics.pageview-url")))
  }

  def append(d: String, t: Long) = Action.async { implicit request =>
    val aianidCokieO = request.cookies.get("aianid")
    val sessidCokieO = request.cookies.get("sessionid")
    val (timestamp, _) = d splitAt 13

    request.headers.get("referer")
      .map { ref =>
        val url = PageURL(ref)
        checkAndCreateCookies(aianidCokieO, sessidCokieO, url, timestamp.toLong)
          .map { case (aianid, sessionid) =>
            notification ! AddToCSV(t, aianid.toLong, sessionid.toLong, url, d)
            createResponseWithCookies(aianid, sessionid)
          }
      }
      .getOrElse {
        Logger.warn(s"Referer is not present in header for data [$d] and token id [$t]")
        Future.successful(Ok(""))
      }
      .recover {
        case ex: Exception =>
          Logger.warn(s"Error occurred while setting cookies. Exception: [$ex]")
          Ok("")
      }
  }

  def pageView(ts: Long) = Action.async { implicit request =>
    val aianidCokieO = request.cookies.get("aianid")
    val sessidCokieO = request.cookies.get("sessionid")

    request.headers.get("referer")
      .map { ref =>
        val url = PageURL(ref)
        checkAndCreateCookies(aianidCokieO, sessidCokieO, url, ts)
          .map { case (aianid, sessionid) =>
            createResponseWithCookies(aianid, sessionid)
          }
      }
      .getOrElse {
        Logger.warn(s"Referer is not present in header while setting the cookie")
        Future.successful(Ok(""))
      }
      .recover {
        case ex: Exception =>
          Logger.warn(s"Error occurred while setting cookies. Exception: [$ex]")
          Ok("")
      }
  }

  private def createResponseWithCookies(aianid: String, sessionid: String) = {
    Ok("").withCookies(
      Cookie("aianid", aianid, Option(config.getInt("cookie.aianid.max-age"))),
      Cookie("sessionid", sessionid, Option(config.getInt("cookie.sessionid.max-age")))
    )
  }

  private def checkAndCreateCookies(aianidCokieO: Option[Cookie], sessidCokieO: Option[Cookie], pageUrl: PageURL, timestamp: Long) =
    (aianidCokieO, sessidCokieO) match {
      case (None, _) =>
        implicit val timeout = Timeout(2 seconds)
        for {
          aianid    <- idGenerationService ?= GetAianId
          sessionid <- idGenerationService ?= GetSessionId
          // _         <- notification ?= AddSession(aianid, sessionid, pageUrl, timestamp)
        } yield (aianid.toString, sessionid.toString)

      case (Some(aianidcookie), Some(sessionidcookie)) =>
        Future.successful((aianidcookie.value, sessionidcookie.value))

      case (Some(aianidcookie), None) =>
        implicit val timeout = Timeout(2 seconds)
        for {
          sessionid <- idGenerationService ?= GetSessionId
          // _         <- notification ?= AddSession(aianidcookie.value.toLong, sessionid, pageUrl, timestamp)
        } yield (aianidcookie.value, sessionid.toString)
    }

}