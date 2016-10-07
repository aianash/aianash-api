package controllers.trail

// import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import scala.collection.mutable.{ Map}

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
  var stats = Seq.empty[Map[String, (String, Float)]]
  for(i <- 1 to 7) {
    val total = math.abs(Random.nextLong) % 10000
    val per = (Random.nextFloat % 0.5) + 0.3
    val newu = total * per
    val drop = total * (1 - per)
    stats = stats :+ Map("new" -> (prettyCount(newu.toLong), (newu * 100/ total).toFloat),
        "drop" -> (prettyCount(drop.toLong), (drop * 100/ total).toFloat))
  }

  val trail = Seq(
    Json.obj(
      "name" -> "clicked-subscribe",
      "new" -> Json.obj(
        "count" -> stats(0).get("new").get._1,
        "percent" -> stats(0).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(0).get("drop").get._1,
        "percent" -> stats(0).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-how-to-integrate",
      "new" -> Json.obj(
        "count" -> stats(1).get("new").get._1,
        "percent" -> stats(1).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(1).get("drop").get._1,
        "percent" -> stats(1).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-behavior",
      "new" -> Json.obj(
        "count" -> stats(2).get("new").get._1,
        "percent" -> stats(2).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(2).get("drop").get._1,
        "percent" -> stats(2).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-how-it-works",
      "props" -> Json.obj(
        "time" -> "> 20s"
      ),
      "new" -> Json.obj(
        "count" -> stats(3).get("new").get._1,
        "percent" -> stats(3).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(3).get("drop").get._1,
        "percent" -> stats(3).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-advantages",
      "props" -> Json.obj(
        "time" -> "< 10s",
        "focus-on" -> "our-features"
      ),
      "new" -> Json.obj(
        "count" -> stats(4).get("new").get._1,
        "percent" -> stats(4).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(4).get("drop").get._1,
        "percent" -> stats(4).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-description",
      "props" -> Json.obj(
        "time" -> "< 20s",
        "focus-on" -> "images"
      ),
      "new" -> Json.obj(
        "count" -> stats(5).get("new").get._1,
        "percent" -> stats(5).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(5).get("drop").get._1,
        "percent" -> stats(5).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    ),
    Json.obj(
      "name" -> "viewed-top-page",
      "props" -> Json.obj(
        "time" -> "< 10s",
        "focus-on" -> "text"
      ),
      "new" -> Json.obj(
        "count" -> stats(6).get("new").get._1,
        "percent" -> stats(6).get("new").get._2
      ),
      "drop" -> Json.obj(
        "count" -> stats(6).get("drop").get._1,
        "percent" -> stats(6).get("drop").get._2
      ),
      "total" -> prettyCount(math.abs(Random.nextLong) % 10000)
    )
  )

  trails += ("original" -> trail)

  var i = 1
  trail.foreach((node) => {
    var (diff, same) = trail.splitAt(i)
    i = i + 1
    var isFirst = true
    val forkedAt = (node \ "name").as[String]
    diff.reverse.foreach((a) => {
      val nodename = (a \ "name").as[String]
      if(Random.nextFloat < 0.9 && nodename != "clicked-subscribe" && nodename != forkedAt) {
        if(isFirst) {
          same = (node + ("diverging" -> JsBoolean(true))) +: same
          same = a +: same
          isFirst = false
        }
        else{
          same = a +: same
        }
      }
    })
    trails += (forkedAt -> same)
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
      "events" -> Json.arr("clicked-subscribe", "viwed-how-to-integrate", "viwed-behavior", "viwed-how-it-works", "viwed-advantages", "viwed-homepage")
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