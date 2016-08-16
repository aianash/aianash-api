package controllers.analytics

import play.api._
import play.api.mvc._
import views.html._

//
class AnalyticsController extends Controller {

  def aianashjs = Action { implicit request =>
    Ok(views.js.aianash())
  }

}