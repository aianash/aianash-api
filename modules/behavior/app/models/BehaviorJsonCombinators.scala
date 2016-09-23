package models.behavior

import java.net.URL

import play.api.libs.json._
import play.api.libs.functional.syntax._

import org.joda.time.Duration

import aianash.commons.behavior._


//
trait BehaviorJsonCombinator {
  import Behavior._

  protected def roundOff(value: Double, to: Int = 3) =
    math.round(value * math.pow(10.0, to)) / 10.0

  protected def prettyCount(count: Long) = {
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

  implicit val distributionParamsFormat: Writes[DistributionParams] = (
    (__ \ "mean").write[Double] and
    (__ \ "var").write[Double]
  ) (
    (params: DistributionParams) => (roundOff(params.mean), roundOff(params.variance))
  )

  implicit val informationFormat: Writes[Information] = (
    (__ \ "prior").write[Map[String, DistributionParams]] and
    (__ \ "posterior").write[Map[String, DistributionParams]]
  ) (
    (information: Information) => (information.prior.prob, information.posterior.prob)
  )

  implicit val timelineStatsFormat: Writes[TimelineStats] = (
    (__ \ "reach").write[String] and
    (__ \ "drop").write[String]
  ) (
    (stats: TimelineStats) => prettyCount(stats.reach) -> prettyCount(stats.drop)
  )

  implicit val actionFormat: Writes[Action] = (
    (__ \ "category").write[String] and
    (__ \ "name").write[String] and
    (__ \ "label").write[String] and
    (__ \ "stats").write[Map[String, Map[String, String]]]
  ) (
    (action: Action) => {
      import action._
      (
        category,
        name,
        label,
        stats.mapValues { v =>
          v.map(sl => sl._1 -> prettyCount(sl._2)).toMap
        }
      )
    }
  )

  private implicit val sectionDistributionFormat: Writes[(PageSection, DistributionParams)] = (
    (__ \ "sectionId").write[String] and
    (__ \ "name").write[String] and
    (__ \ "mean").write[Double] and
    (__ \ "var").write[Double]
  ) (
    (sd: (PageSection, DistributionParams)) =>
      (sd._1.sectionId.toString, sd._1.name, roundOff(sd._2.mean), roundOff(sd._2.variance))
  )

  implicit val timelineEventFormat: Writes[TimelineEvent] = (
    (__ \ "durationIntoPage").write[String] and
    (__ \ "sections").write[Map[String, (PageSection, DistributionParams)]] and
    (__ \ "tags").write[Map[String, DistributionParams]] and
    (__ \ "stats").write[TimelineStats] and
    (__ \ "actions").write[Set[Action]]
  ) (
    (event: TimelineEvent) => {
      import event._
      (
        durationIntoPage.getStandardSeconds().toString,
        sections.prob.map(kv => kv._1.toString -> kv._2),
        tags.prob,
        stats, actions
      )
    }
  )

  implicit val storyFormat: Writes[Story] = (
    (__ \ "information").write[Information] and
    (__ \ "timeline").write[IndexedSeq[TimelineEvent]]
  ) (
    (story: Story) => (story.information, story.timeline)
  )

  implicit val referralFormat: Writes[Referral] = (
    (__ \ "pageId").write[String] and
    (__ \ "name").write[String] and
    (__ \ "count").write[String] and
    (__ \ "url").write[String]
  ) (
    (referral: Referral) => {
      import referral._
      (pageId.toString, name, prettyCount(count), url.toString)
    }
  )

  implicit val statsFormat: Writes[Stats] = (
    (__ \ "pageViews").write[String] and
    (__ \ "totalVisitors").write[String] and
    (__ \ "newVisitors").write[String] and
    (__ \ "avgDwellTime").write[String] and
    (__ \ "previousPages").write[Seq[Referral]] and
    (__ \ "nextPages").write[Seq[Referral]]
  ) (
    (stats: Stats) => {
      import stats._
      (
        prettyCount(pageViews.count),
        prettyCount(totalVisitors.count),
        prettyCount(newVisitors.count),
        avgDwellTime.getStandardSeconds() + "s",
        previousPages,
        nextPages
      )
    }
  )

  implicit val behaviorFormat: Writes[Behavior] = (
    (__ \ "behaviorId").write[String] and
    (__ \ "name").write[String]
  ) (
    (behavior: Behavior) => (behavior.behaviorId.bhuuid.toString, behavior.name)
  )
}