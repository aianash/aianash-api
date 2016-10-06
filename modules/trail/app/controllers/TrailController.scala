package controllers.trail

// import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import scala.collection.mutable.{Seq, Map}

import javax.inject._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import aianonymous.commons.core.protocols._, Implicits._
import aianonymous.commons.core.PageURL

// import aianash.commons.trail._

import actors.trail._
// import models.trail._

import org.joda.time.Duration


@Singleton
class TrailController @Inject() (system: ActorSystem,
  @Named(TrailClient.name) client: ActorRef) extends Controller {

  var trails = Map.empty[String, Seq[JsObject]]

  val trail = Seq(
    Json.obj(
      "name" -> "subscribe",
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "how-to-integrate",
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "behavior",
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "how-it-works",
      "props" -> Json.obj(
        "time" -> "> 20s"
      ),
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "advantages",
      "props" -> Json.obj(
        "time" -> "< 10s",
        "focus-on" -> "our-features"
      ),
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "homepage",
      "props" -> Json.obj(
        "time" -> "< 20s",
        "focus-on" -> "images"
      ),
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "homepage",
      "props" -> Json.obj(
        "time" -> "< 10s",
        "focus-on" -> "text"
      ),
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    )
  )

  trails += ("original" -> trail)

  var i = 1
  trail.foreach((node) => {
    var (same, diff) = trail.splitAt(i)
    i = i + 1
    var j = 0
    var isFirst = true
    val forkedAt = (node \ "name").as[String]
    same.foreach((a) => {
      val nodename = (a \ "name").as[String]
      if(Random.nextFloat < 0.8 && nodename != "behavior") {
        if(isFirst) {
          diff = (same(j) + ("divergedFrom" -> JsString(forkedAt))) +: diff
          isFirst = false
        }
        else{
          diff = same(j) +: diff
        }
      }
      j = j + 1
    })
    trails += (forkedAt -> diff)
  })

  private def roundOff(value: Double, to: Int = 3) =
    math.round(value * math.pow(10.0, to)) / 10.0

  private def prettyCount(count: Long) = {
    if(count <= 1000) count.toString
    else {
      var p = 10000L
      while(count / p != 0) {
        p *= 10L
      }
      var suffix = ""
      if(p >= 10000L && p < 10000000L) {
        p = 1000L
        suffix = "K"
      } else {
        p = 1000000L
        suffix = "M"
      }

      val value = (count / p.asInstanceOf[Double])
      (math.round(value * 100.0) / 100.0).toString + suffix
    }
  }

  def trail(tokenId: Long) = Action.async(parse.json) { implicit request =>
    val timeline = trails.get("original")
    val response = Json.obj(
      "trail" -> Json.obj(
        "timeline" -> timeline,
        "timeseries" -> (for(_ <- 1 to 14) yield roundOff(math.abs(Random.nextGaussian) % 1.0, 1))
      )
    )

    Future(Ok(response))
  }

  def fork(tokenId: Long) = Action.async(parse.json) { implicit request =>
    val forkedAt = (request.body \ "fork").as[String]
    val timeline = trails.get(forkedAt)
    val response = Json.obj(
      "trail" -> Json.obj(
        "isfork" -> true,
        "divergedFrom" -> forkedAt,
        "timeline" -> timeline,
        "timeseries" -> (for(_ <- 1 to 14) yield roundOff(math.abs(Random.nextGaussian) % 1.0, 1))
      )
    )

    Future(Ok(response))
  }

  def getEvents(tokenId: Long) = Action.async { implicit request =>
    val response = Json.obj(
      "events" -> Json.arr("subscribe", "how-to-integrate", "behavior", "how-it-works", "advantages", "homepage")
    )
    Future(Ok(response))
  }

  def getEventProperties(tokenId: Long, name: String) = Action.async { implicit request =>
    val response = Json.obj(
      "event" -> Json.obj(
        "name" -> name,
        "props" -> Json.obj(
          "time" -> Json.obj(
            "type" -> "String"
          ),
          "focus-on" -> Json.obj(
            "type" -> "String"
          )
        )
      )
    )
    Future(Ok(response))
  }
}