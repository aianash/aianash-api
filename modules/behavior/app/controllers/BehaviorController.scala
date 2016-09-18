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
import models.behavior._

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
    Behavior.Referral(1L, "Home page", 100L, new URL("http://aianash.com")),
    Behavior.Referral(2L, "Behavior page", 200L, new URL("http://aianash.com/dashboard/behavior")),
    Behavior.Referral(3L, "Predict page", 100L, new URL("http://aianash.com/dashboard/predict")),
    Behavior.Referral(4L, "AB Test page", 300L, new URL("http://aianash.com/dashboard/abtest"))
  )

  private def mkstats = {
    import Behavior._
    Stats(
      PageViews(103000L),
      Visitors(50400L),
      Visitors(5200L),
      new Duration(10000),
      referrals.map(_.copy(count = 100)),
      referrals.map(_.copy(count = 30))
    )
  }

  //
  def getCluster(tokenId: Long, pageId: Long, instanceId: String) =
    Action.async { implicit request =>
      import Behavior._

      val cluster =
        Json.obj(
          "cluster" -> Json.toJson(behaviors)
        )

      Future(Ok(cluster))
    }

  //
  def getStory(tokenId: Long, pageId: Long, instanceId: String, behaviorId: Long) =
    Action.async { implicit request =>
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
        (1 to 10).foldLeft(IndexedSeq.newBuilder[TimelineEvent]) { (seq, idx) =>
          val duration = new Duration(Random.nextLong)
          val sectionsD = SectionDistribution(
            sections.map { section =>
              section.sectionId -> (section, DistributionParams(Random.nextDouble, Random.nextDouble))
            } toMap
          )

          val tagsD = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
          val stats = TimelineStats(100050L, 154000L)

          seq += TimelineEvent(duration, sectionsD, tagsD, stats, Set.empty[Action])
        } result()

      val res = Json.obj(
        "story" -> Json.toJson(Story(information, timeline))
      )
      Future(Ok(res))
    }

  //
  def getAllStats(tokenId: Long, pageId: Long, instanceId: String) =
    Action.async { implicit request =>
      import Behavior._

      val stats = Json.obj(
        "stats" -> Json.toJson(mkstats)
      )

      Future(Ok(stats))
    }

  //
  def getStat(tokenId: Long, pageId: Long, instanceId: String, behaviorId: Long) =
    Action.async { implicit request =>
      import Behavior._

      val stats = Json.obj(
        "stat" -> Json.toJson(mkstats)
      )

      Future(Ok(stats))
    }

}