package controllers.customer

import scala.concurrent.duration._
import scala.concurrent.Future

import java.net.URL

import javax.inject._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.Configuration

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL
import aianonymous.commons.customer.{PageTags, CustomerJsonCombinator}

import actors.customer._


@Singleton
class CustomerSettingsController @Inject() (system: ActorSystem, config: Configuration,
  @Named(CustomerConfigurator.name) configurator: ActorRef) extends Controller with CustomerJsonCombinator {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def addPageTags = Action.async(parse.json) { implicit request =>
    val tid  = (request.body \ "token_id").as[String]
    val url  = (request.body \ "url").as[String]
    val tags = (request.body \ "tags").as[Seq[JsValue]]
    val name = (request.body \ "name").asOpt[String] getOrElse config.getString("webpage.default-name")

    val pageurl = PageURL(url)
    val tokenId = tid.toLong

    implicit val timeout = Timeout(2 seconds)
    (configurator ?= ObtainOrCreatePageId(pageurl, tokenId, name)).map { pageId =>
      tags map { tag =>
        val sid = (tag \ "section_id").as[Int]
        val t   = (tag \ "tags").as[Set[String]]
        PageTags(tokenId, pageId, sid, t)
      }
    } flatMap { tags =>
      (configurator ?= AddPageTags(tags)).map { res =>
        if(res == true) Ok("Tags have been added successfully")
        else InternalServerError("Some error occurred !")
      }
    }
  }

  def getPageTags(tokenId: Long, url: String) = Action.async { implicit request =>
    implicit val timeout = Timeout(2 seconds)
    val pageurl = PageURL(url)
    (configurator ?= ObtainPageId(pageurl)) flatMap {
      case Some(pageId) =>
        (configurator ?= ObtainPageTags(tokenId, pageId)).map { res =>
          Ok(Json.toJson(res))
        }

      case None =>
        Future.successful(Ok(Json.toJson(Seq.empty[PageTags])))
    }
  }

  def getTokenId(url: String) = Action.async { implicit request =>
    val pageurl = PageURL(url)
    val name = pageurl.host
    implicit val timeout = Timeout(2 seconds)
    (configurator ?= ObtainTokenId(name)).map { res =>
      res match {
        case Some(domain) => Ok(Json.toJson(domain))
        case None         => Unauthorized("Sorry ! This domain is not in our database.")
      }
    }
  }

}