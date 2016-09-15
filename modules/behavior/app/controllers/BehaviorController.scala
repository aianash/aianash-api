package controllers.behavior

// import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

import java.net.URL

import javax.inject._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import aianonymous.commons.core.protocols._, Implicits._
import aianash.commons.behavior._

import actors.behavior._

import org.joda.time.Duration


@Singleton
class BehaviorController @Inject() (system: ActorSystem,
  @Named(BehaviorClient.name) client: ActorRef) extends Controller with BehaviorJsonCombinator {

  private val behaviors = Seq(
    Behavior(BehaviorId(1L), "morning user"),
    Behavior(BehaviorId(2L), "afternoor user"),
    Behavior(BehaviorId(3L), "evening user"),
    Behavior(BehaviorId(4L), "night user")
  )

  private val referrals = Seq(
    Behavior.Referral(1L, "Home page", 1.0f, new URL("http://aianash.com")),
    Behavior.Referral(1L, "Behavior page", 1.0f, new URL("http://aianash.com/dashboard/behavior")),
    Behavior.Referral(1L, "Predict page", 1.0f, new URL("http://aianash.com/dashboard/predict")),
    Behavior.Referral(1L, "AB Test page", 1.0f, new URL("http://aianash.com/dashboard/abtest"))
  )

  private def mkstats = {
    import Behavior._
    Stats(
      PageViews(Random.nextLong),
      Visitors(Random.nextLong),
      Visitors(Random.nextLong),
      new Duration(Random.nextLong),
      referrals.map(_.copy(score = Random.nextFloat)),
      referrals.map(_.copy(score = Random.nextFloat))
    )
  }

  //
  def getCluster(tokenId: Long, pageId: Long, instanceId: Long) =
    Action.async(parse.json) { implicit request =>
      import Behavior._

      val cluster =
        Json.obj(
          "cluster" -> Json.toJson(behaviors)
        )

      Future(Ok(cluster))
    }

  //
  def getStory(tokenId: Long, pageId: Long, instanceId: Long, behaviorId: Long) =
    Action.async(parse.json) { implicit request =>
      import Behavior._
      val tags = List("web", "analytics", "artificial-intelligence", "user-behavior")

      val prior = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
      val posterior = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
      val information = Information(prior, posterior)

      val sections =
        (1 to 4).foldLeft(Seq.newBuilder[PageSection]) { (seq, idx) =>
          seq += PageSection(idx, "section-" + idx)
        } result()

      val timeline =
        (1 to 10).foldLeft(IndexedSeq.newBuilder[Timeline]) { (seq, idx) =>
          val duration = new Duration(Random.nextLong)
          val sectionsD = SectionDistribution(
            sections.map { section =>
              section.sectionId -> (section, DistributionParams(Random.nextDouble, Random.nextDouble))
            } toMap
          )

          val tagsD = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
          val stats = TimelineStats(Random.nextLong, Random.nextLong)

          seq += Timeline(duration, sectionsD, tagsD, stats, Set.empty[Action])
        } result()

      Future(Ok(Json.toJson(Story(information, timeline))))
    }

  //
  def getAllStats(tokenId: Long, pageId: Long, instanceId: Long) =
    Action.async(parse.json) { implicit request =>
      import Behavior._

      val behaviorsS =
        behaviors.map(_.behaviorId.bhuuid.toString -> Json.toJson(mkstats)).toMap

      val stats = Json.obj(
        "stats" -> Json.toJson(mkstats),
        "behaviors" -> Json.toJson(behaviorsS)
      )

      Future(Ok(stats))
    }

  //
  def getStat(tokenId: Long, pageId: Long, instanceId: Long, behaviorId: Long) =
    Action.async(parse.json) { implicit request =>
      import Behavior._

      val stats = Json.obj(
        "stats" -> Json.toJson(mkstats)
      )

      Future(Ok(""))
    }

}