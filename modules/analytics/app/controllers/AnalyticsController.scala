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
    Ok("")
  }

}