package controllers.customer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.net.URL

import javax.inject._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.api.libs.json._

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.customer.{PageTags, CustomerJsonCombinator}

import actors.customer._


@Singleton
class CustomerSettingsController @Inject() (system: ActorSystem,
  @Named(CustomerConfigurator.name) pageTagger: ActorRef) extends Controller with CustomerJsonCombinator {

  def addPageTags = Action.async(parse.json) { implicit request =>
    val tid  = (request.body \ "token_id").as[String]
    val url  = (request.body \ "url").as[String]
    val tags = (request.body \ "tags").as[Seq[JsValue]]

    val urlo = new URL(url)
    val urln = urlo.getHost + ":" + urlo.getPort + urlo.getPath

    implicit val timeout = Timeout(2 seconds)
    (pageTagger ?= GetPageId(urln, tid.toLong)).map { pageUrl =>
      tags map { tag =>
        val sid = (tag \ "section_id").as[Int]
        val t   = (tag \ "tags").as[Set[String]]
        PageTags(pageUrl.tokenId, pageUrl.pageId, sid, t)
      }
    } flatMap { tags =>
      (pageTagger ?= AddPageTags(tags)).map { res =>
        if(res == true) Ok("Tags have been added successfully")
        else InternalServerError("Some error occurred !")
      }
    }
  }

  def getPageTags(tokenId: Long, url: String) = Action.async { implicit request =>
    implicit val timeout = Timeout(2 seconds)
    val urlo = new URL(url)
    val urln = urlo.getHost + ":" + urlo.getPort + urlo.getPath
    (pageTagger ?= GetPageId(urln, tokenId)) flatMap { pageUrl =>
      (pageTagger ?= GetPageTags(tokenId, pageUrl.pageId)).map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def getTokenId(url: String) = Action.async { implicit request =>
    val urlo  = new URL(url)
    val name = urlo.getHost
    implicit val timeout = Timeout(2 seconds)
    (pageTagger ?= GetTokenId(name)).map { res =>
      res match {
        case Some(domain) => Ok(Json.toJson(domain))
        case None         => Unauthorized("Sorry ! This domain is not in our database.")
      }
    }
  }

}