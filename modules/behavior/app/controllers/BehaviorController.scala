package controllers.behavior

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

import aianash.commons.behavior._

import actors.behavior._
import models.behavior._

import org.joda.time.Duration


@Singleton
class BehaviorController @Inject() (system: ActorSystem,
  @Named(BehaviorClient.name) client: ActorRef) extends Controller with BehaviorJsonCombinator {

  private val behaviors = Seq(
    Behavior(BehaviorId(1L), "New Behavior"),
    Behavior(BehaviorId(2L), "Recent Users"),
    Behavior(BehaviorId(3L), "Short lived Users"),
    Behavior(BehaviorId(4L), "Highest ROI")
  )

  private val referrals = Seq(
    Behavior.Referral(1L, "Home page", 100L,  PageURL("http://aianash.com")),
    Behavior.Referral(2L, "Behavior page", 200L, PageURL("http://aianash.com/dashboard/behavior")),
    Behavior.Referral(3L, "Predict page", 100L, PageURL("http://aianash.com/dashboard/predict")),
    Behavior.Referral(4L, "AB Test page", 300L, PageURL("http://aianash.com/dashboard/abtest"))
  )

  private val tags = List("web", "analytics", "artificial-intelligence", "user-behavior")

  private val tv = 13000L
  private val nv = 1100L
  private val pv = 32500L
  private val adt = 50000L

  private def mkstats(ratio: Double, adt: Long, visit: Int) = {
    import Behavior._
    Stats(
      PageViews((pv.toDouble * ratio).toLong),
      Visitors((tv.toDouble * ratio).toLong),
      Visitors((nv.toDouble * ratio).toLong),
      new Duration(adt),
      referrals.map(_.copy(count = math.abs(Random.nextInt() % visit))),
      referrals.map(_.copy(count = math.abs(Random.nextInt() % visit)))
    )
  }


  private val pgstats = mkstats(1.0, adt, 1000)
  private val b1stat = mkstats(.1, 45000, 300)
  private val b2stat = mkstats(.2, 50000, 300)
  private val b3stat = mkstats(.3, 30000, 300)
  private val b4stat = mkstats(.4, 60000, 300)

  private def bstats(behaviorId: Long) =
    behaviorId match {
      case 1L => b1stat
      case 2L => b2stat
      case 3L => b3stat
      case 4L => b4stat
    }

  private val sectionNames = Seq(
    "product section",
    "feature section",
    "detail section",
    "video section",
    "detail section",
    "feature section",
    "video section",
    "product section"
  )

  private def mkInformation = {
    import Behavior._
    val prior = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
    val posterior = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
    Information(prior, posterior)
  }

  private val informations =
    behaviors.foldLeft(Map.newBuilder[BehaviorId, Behavior.Information]) { (map, behavior) =>
      map += behavior.behaviorId -> mkInformation
    } result()


  private def binfor(behaviorId: BehaviorId) =
    informations(behaviorId)

  private val actions = Seq("click", "bought", "play")
  private def mkAction() = {
    Behavior.Action("product", actions(math.abs(Random.nextInt()) % 3), "interested in product", Map("plan" -> Seq("premium" -> math.abs(Random.nextInt() % 10000))))
  }

  //
  private val stories = {
    import Behavior._
    behaviors.foldLeft(Map.newBuilder[BehaviorId, Story]) { (map, behavior) =>
      val sections =
        (1 to 4).foldLeft(Seq.newBuilder[PageSection]) { (seq, idx) =>
          seq += PageSection(idx, sectionNames(math.abs(Random.nextInt()) % sectionNames.size))
        } result()

      val total = bstats(behavior.behaviorId.bhuuid).totalVisitors.count

      val timeline =
        (1 to 10).foldLeft(IndexedSeq.newBuilder[TimelineEvent]) { (seq, idx) =>
          val reach = total.toDouble * math.exp(-(idx - 1).toDouble)
          val drop = reach - (total.toDouble * math.exp(-idx.toDouble))
          if(reach.toLong == 0) seq
          else {
            val duration = new Duration(Random.nextLong)
            val sectionsD = SectionDistribution(
              sections.map { section =>
                section.sectionId -> (section, DistributionParams(Random.nextDouble, Random.nextDouble))
              } toMap
            )

            val tagsD = TagDistribution(tags.map(_ -> DistributionParams(Random.nextDouble, Random.nextDouble)).toMap)
            val stats = TimelineStats(reach.toLong, drop.toLong)

            seq += TimelineEvent(duration, sectionsD, tagsD, stats, Set(mkAction(), mkAction(), mkAction()))
          }
        } result()

      map += behavior.behaviorId -> Story(binfor(behavior.behaviorId), timeline)
    } result()
  }


  //
  def getCluster(tokenId: Long, pageId: Long, instanceId: String) =
    Action.async { implicit request =>
      import Behavior._

      val cluster =
        Json.obj(
          "cluster" -> behaviors.map { behavior =>
            Json.toJson(behavior).asInstanceOf[JsObject] ++
              Json.obj("visitors" -> (behavior.behaviorId.bhuuid * 10))
          }
        )

      Future(Ok(cluster))
    }

  //
  def getInformations(tokenId: Long, pageId: Long, instanceId: String) =
    Action.async { implicit request =>
      import Behavior._

      val informations =
        behaviors.map { behavior =>
          Json.obj(
            "behaviorId" -> behavior.behaviorId.bhuuid.toString,
            "name" -> behavior.name,
            "information" -> Json.toJson(binfor(behavior.behaviorId)),
            "stat" -> bstats(behavior.behaviorId.bhuuid)
          )
        }

      val information = Json.obj(
        "information" -> Json.toJson(informations)
      )
      Future(Ok(information))
    }

  //
  def getStory(tokenId: Long, pageId: Long, instanceId: String, behaviorId: Long) =
    Action.async { implicit request =>
      import Behavior._
      val res = Json.obj(
        "story" -> Json.toJson(stories(BehaviorId(behaviorId)))
      )
      Future(Ok(res))
    }

  //
  def getAllStats(tokenId: Long, pageId: Long, instanceId: String) =
    Action.async { implicit request =>
      import Behavior._

      val stats = Json.obj(
        "stats" -> Json.toJson(pgstats)
      )

      Future(Ok(stats))
    }

  //
  def getStat(tokenId: Long, pageId: Long, instanceId: String, behaviorId: Long) =
    Action.async { implicit request =>
      import Behavior._
      val stats = Json.obj(
        "stat" -> Json.toJson(bstats(behaviorId))
      )
      Future(Ok(stats))
    }

}