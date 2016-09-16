package actors.customer

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class CustomerModule extends AbstractModule with AkkaGuiceSupport {

  def configure = {
    bindActor[CustomerConfigurator](CustomerConfigurator.name)
  }

}
