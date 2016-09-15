package actors.behavior

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.behavior._


class BehaviorModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[BehaviorClient](BehaviorClient.name)
  }
}
