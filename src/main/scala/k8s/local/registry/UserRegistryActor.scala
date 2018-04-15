package k8s.local.registry

import akka.actor.{Actor, ActorLogging, Props}
import com.redis._
import k8s.local.tools.JsonSupport
import spray.json._

final case class User(username: String, password: String, name: String)

final case class Users(users: List[User])

object UserRegistryActor {

  final case object GetUsers

  final case class ActionPerformed(description: String)

  final case class CreateUser(user: User)

  final case class GetUser(name: String)

  def props: Props = Props[UserRegistryActor]
}

class UserRegistryActor extends Actor with JsonSupport with ActorLogging {

  import UserRegistryActor._

  val users = List.empty[User]
  private val redis = new RedisClient(sys.env("REDIS_URL"), sys.env("REDIS_PORT").toInt)

  def receive: Receive = {

    case CreateUser(user) =>
      redis.set("users:" + user.username, user.toJson.toString)
      sender() ! ActionPerformed(s"User ${user.name} created.")

    case GetUser(username) =>
      sender() ! redis.get[String]("users:" + username)
        .map(user => user.parseJson.convertTo[User])
        .getOrElse(Option.empty[User])
  }
}
