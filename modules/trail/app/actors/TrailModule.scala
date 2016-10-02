package actors.trail

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.trail._


class TrailModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[TrailClient](TrailClient.name)
  }
}
