package actors.user

import javax.inject._

import akka.actor.{Actor, ActorLogging, Props}

import play.Configuration

import com.ecwid.maleorang.{MailchimpClient, MailchimpException}
import com.ecwid.maleorang.method.v3_0.members.EditMemberMethod

//
sealed trait UserServiceProtocol
case class AddSubscription(email: String) extends UserServiceProtocol

//
class UserService @Inject() (config: Configuration) extends Actor with ActorLogging {

  val apiKey = config.getString("mailchimp.api-key")
  val listId = config.getString("mailchimp.list-id")
  val mailchimpClient = new MailchimpClient(apiKey)

  def receive = {
    case AddSubscription(email) =>
      createOrUpdateSubscription(email)

    case _ =>
      log.error("Invalid message !")
  }

  private def createOrUpdateSubscription(email: String) {
    try {
      val method = new EditMemberMethod.Create(listId, email)
      method.status = "pending"

      mailchimpClient.execute(method)
    } catch {
      case ex: MailchimpException if(ex.code == 400) =>
        log.warning("Email [{}] already subscribed", email)

      case ex: Exception =>
        log.error(ex, "Error while adding email [{}] to subscription list", email)
    }
  }

}

//
object UserService {

  final val name = "user-service"

  def props = Props(classOf[UserService])

}