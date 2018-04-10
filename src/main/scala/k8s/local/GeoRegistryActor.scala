package k8s.local
import akka.actor.{ Actor, ActorLogging, Props }
import com.redis._
import spray.json._

final case class Geo(name: String, x: Float, y: Float)
final case class Geos(users: List[Geo])

object GeoRegistryActor {

  final case object GetGeos
  final case class GeoPerformed(description: String)
  final case class CreateGeo(geo: Geo)
  final case class GetGeo(name: String)
  def props: Props = Props[GeoRegistryActor]
}

class GeoRegistryActor extends Actor with JsonSupport with ActorLogging {

  import GeoRegistryActor._

  val users = List.empty[User]
  private val redis = new RedisClient("localhost", 6379)

  def receive: Receive = {
    case GetGeos =>
      sender() ! Geos(redis.lrange[String]("geo", 0, 100)
        .getOrElse(List.empty[Option[Geo]])
        .map({
          case Some(test: String) => test.parseJson.convertTo[Geo]
        }))

    case CreateGeo(geo) =>
      redis.geoadd("maps", Seq((geo.x, geo.y, geo.name)))
      sender() ! GeoPerformed(s"geo ${geo.name} created.")

    case GetGeo(name) =>
      sender() ! redis.lrange[String]("geo", 0, 100)
        .getOrElse(List.empty[Option[Geo]])
        .map({
          case Some(test: String) => test.parseJson.convertTo[Geo]
        })
        .find(el => el.name == name)
  }
}
