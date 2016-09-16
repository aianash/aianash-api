package actors.user

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

//
class UserModule extends AbstractModule with AkkaGuiceSupport {

  def configure = {
    bindActor[UserService](UserService.name)
  }

}
