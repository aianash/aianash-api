package controllers.demo

import play.api._
import play.api.mvc._
import views.html._

//
class DemoController extends Controller {

  def techProduct = Action { implicit request =>
    // Ok("hello")
    Ok(views.html.techProduct())
  }

}