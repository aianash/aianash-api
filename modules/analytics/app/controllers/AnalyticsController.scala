package controllers.analytics

import scala.concurrent.duration._
import scala.concurrent.Future

import javax.inject._
import java.net.URL

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.Configuration
import views.html._

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL

import actors.analytics.{NotificationService, Notify, AddSession}
import actors.analytics.{IdGenerationService, GetAianId, GetSessionId}

//
@Singleton
class AnalyticsController @Inject() (system: ActorSystem, config: Configuration,
    @Named(NotificationService.name) notification: ActorRef,
    @Named(IdGenerationService.name) idGenerationService: ActorRef
  ) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def aianashjs = Action { implicit request =>
    Ok(views.js.aianash())
  }

  def append(d: String, t: Long) = Action.async { implicit request =>
    val aianidcookieO = request.cookies.get("aianid")
    val sessionidcookieO = request.cookies.get("sessionid")

    request.headers.get("referer") match {
      case Some(ref) =>
        val url = PageURL(ref)
        checkAndCreateCookies(aianidcookieO, sessionidcookieO, url) map { case (aianid, sessionid) =>
          notification ! Notify(t, aianid.toLong, sessionid.toLong, url, d)
          Ok("").withCookies(
            Cookie("aianid", aianid, Option(config.getInt("cookie.aianid.max-age"))),
            Cookie("sessionid", sessionid, Option(config.getInt("cookie.sessionid.max-age")))
          )
        } recover {
          case ex: Exception => Ok("")
        }

      case None =>
        Logger.warn(s"Referer is not present in header for data [$d] and token id [$t]")
        Future.successful(Ok(""))
    }
  }

  def setCookies = Action.async { implicit request =>
    val aianidcookieO = request.cookies.get("aianid")
    val sessionidcookieO = request.cookies.get("sessionid")

    request.headers.get("referer") match {
      case Some(ref) =>
        val url = PageURL(ref)
        checkAndCreateCookies(aianidcookieO, sessionidcookieO, url) map { case (aianid, sessionid) =>
          Ok("").withCookies(
            Cookie("aianid", aianid, Option(config.getInt("cookie.aianid.max-age"))),
            Cookie("sessionid", sessionid, Option(config.getInt("cookie.sessionid.max-age")))
          )
        } recover {
          case ex: Exception =>
            Logger.warn(s"Error occurred while setting cookies. Exception: [$ex]")
            Ok("")
        }

      case None =>
        Logger.warn(s"Referer is not present in header while setting the cookie")
        Future.successful(Ok(""))
    }
  }

  private def checkAndCreateCookies(aianidcookieO: Option[Cookie], sessionidcookieO: Option[Cookie], pageUrl: PageURL) =
    (aianidcookieO, sessionidcookieO) match {
      case (None, _) =>
        implicit val timeout = Timeout(2 seconds)
        for {
          aianid    <- idGenerationService ?= GetAianId
          sessionid <- idGenerationService ?= GetSessionId
          _         <- notification ?= AddSession(aianid, sessionid, pageUrl)
        } yield (aianid.toString, sessionid.toString)

      case (Some(aianidcookie), Some(sessionidcookie)) =>
        Future.successful((aianidcookie.value, sessionidcookie.value))

      case (Some(aianidcookie), None) =>
        implicit val timeout = Timeout(2 seconds)
        for {
          sessionid <- idGenerationService ?= GetSessionId
          _         <- notification ?= AddSession(aianidcookie.value.toLong, sessionid, pageUrl)
        } yield (aianidcookie.value, sessionid.toString)
    }

}