package k8s.local
import akka.actor.{ Actor, ActorLogging, Props }
import com.redis._
import spray.json._

final case class User(username: String, name: String)
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
  private val redis = new RedisClient("localhost", 6379)

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(redis.lrange[String]("users", 0, 100)
        .getOrElse(List.empty[Option[User]])
        .map({
          case Some(test: String) => test.parseJson.convertTo[User]
        }))

    case CreateUser(user) =>
      redis.lpush("users", user.toJson.toString)
      sender() ! ActionPerformed(s"User ${user.name} created.")

    case GetUser(username) =>
      sender() ! redis.lrange[String]("users", 0, 100)
        .getOrElse(List.empty[Option[User]])
        .map({
          case Some(test: String) => test.parseJson.convertTo[User]
        })
        .find(el => el.username == username)
  }
}
