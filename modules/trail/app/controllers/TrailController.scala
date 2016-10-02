package controllers.trail

// import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

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
    val timeline = for(_ <- 1 to 14) yield Json.obj(
      "name" -> "played-video",
      "props" -> Json.obj(
        "genre" -> Json.arr("folk", "pop")
      ),
      "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    )
    val response = Json.obj(
      "trail" -> Json.obj(
        "timeline" -> timeline,
        "timeseries" -> (for(_ <- 1 to 14) yield roundOff(math.abs(Random.nextGaussian) % 1.0, 1))
      )
    )

    Future(Ok(response))
  }

  def fork(tokenId: Long) = Action.async(parse.json) { implicit request =>
    val timeline = for(_ <- 1 to 14) yield Json.obj(
        "name" -> "played-video",
        "props" -> Json.obj(
          "genre" -> Json.arr("folk", "pop")
        ),
        "new" -> prettyCount(math.abs(Random.nextLong) % 10000),
        "drop" -> prettyCount(math.abs(Random.nextLong) % 10000),
        "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
      )
    val response = Json.obj(
      "trail" -> Json.obj(
        "isfork" -> true,
        "timeline" -> timeline,
        "timeseries" -> (for(_ <- 1 to 14) yield roundOff(math.abs(Random.nextGaussian) % 1.0, 1))
      )
    )

    Future(Ok(response))
  }

  def getEvents(tokenId: Long) = Action.async { implicit request =>
    val response = Json.obj(
      "events" -> Json.arr("played-video")
    )
    Future(Ok(response))
  }

  def getEventProperties(tokenId: Long, name: String) = Action.async { implicit request =>
    val response = Json.obj(
      "event" -> Json.obj(
        "name" -> "played-video",
        "props" -> Json.obj(
          "genre" -> Json.obj(
            "type" -> "Categorical",
            "values" -> Json.arr("pop", "folk")
          )
        )
      )
    )
    Future(Ok(response))
  }
}