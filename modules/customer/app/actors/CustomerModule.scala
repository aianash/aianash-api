package actors.customer

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.customer._


class CustomerModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[PageTagger](PageTagger.name)
  }
}
