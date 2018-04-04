package k8s.local

//#user-registry-actor
import akka.actor.{Actor, ActorLogging, Props}
import scredis.Redis
import spray.json._

//#user-case-classes
final case class User(name: String, age: Int, countryOfResidence: String)

final case class Users(users: Seq[User])

//#user-case-classes

object UserRegistryActor {

  final case class ActionPerformed(description: String)

  final case object GetUsers

  final case class CreateUser(user: User)

  final case class GetUser(name: String)

  final case class DeleteUser(name: String)

  def props: Props = Props[UserRegistryActor]
}

class UserRegistryActor extends Actor with JsonSupport with ActorLogging {

  import UserRegistryActor._
  import akka.pattern.pipe
  import context.dispatcher

  var users = Set.empty[User]
  val redis = Redis()


  def receive: Receive = {
    case GetUsers =>
      val ft = redis.lRange[String]("users", 0, 100)  map  {
        values : List[String] => Users(values.map(value => value.parseJson.convertTo[User]))
      }
      ft pipeTo sender()
    case CreateUser(user) =>
      redis.lPush("users", user.toJson.toString)
      sender() ! ActionPerformed(s"User ${user.name} created.")
    case GetUser(name) =>
      sender() ! users.find(_.name == name)
    case DeleteUser(name) =>
      users.find(_.name == name) foreach { user => users -= user }
      sender() ! ActionPerformed(s"User ${name} deleted.")
  }
}

//#user-registry-actor