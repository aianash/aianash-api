package actors.analytics

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class AnalyticsModule extends AbstractModule with AkkaGuiceSupport {

  def configure = {
    bindActor[NotificationService](NotificationService.name)
  }

}
