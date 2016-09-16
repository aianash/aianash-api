package controllers.user

import javax.inject._

import akka.actor.{ActorSystem, ActorRef}

import play.api._
import play.api.mvc._
import views.html._

import actors.user._

//
@Singleton
class UserController @Inject() (system: ActorSystem,
  @Named(UserService.name) userService: ActorRef) extends Controller {

  def subscribe = Action(parse.json) { implicit request =>
    val email = (request.body \ "email").as[String]
    userService ! AddSubscription(email)
    Ok("Subsciption successful !")
  }

}