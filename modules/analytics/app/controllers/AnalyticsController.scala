package controllers.analytics

import javax.inject._
import java.net.URL

import akka.actor.{ActorSystem, ActorRef}

import play.api._
import play.api.mvc._
import views.html._

import actors.analytics.{NotificationService, Notify}

//
@Singleton
class AnalyticsController @Inject() (system: ActorSystem,
  @Named(NotificationService.name) notification: ActorRef) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def aianashjs = Action { implicit request =>
    Ok(views.js.aianash())
  }

  def append(d: String, t: Long, u: String) = Action { implicit request =>
    notification ! Notify(t, 1L, 1L, new URL(u), d)
    Ok("").withHeaders(
      allow(),
      allowOrigin(request),
      allowMethods(),
      allowCredentials(),
      exposedHeaders()
    )
  }

  private def allow() = "Allow" -> "*"

  private def allowOrigin(request: RequestHeader) = {
    val protocol = if (request.secure) "https://" else "http://"
    val origin =  protocol + request.host
    "Access-Control-Allow-Origin" -> origin
  }

  private def allowMethods() = "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS"

  private def exposedHeaders() = "Access-Control-Expose-Headers" -> "WWW-Authenticate, Server-Authorization"

  private def allowCredentials() = "Access-Control-Allow-Credentials" -> "true"
}