package k8s.local.registry

import java.util.UUID.randomUUID

import akka.actor.{ Actor, ActorLogging, Props }
import com.redis._
import k8s.local.dto.{ GeoRequest, UserGeo }
import k8s.local.tools.JsonSupport
import spray.json._

final case class Geo(uuid: String, x: Float, y: Float)

final case class Geos(users: List[UserGeo])

object GeoRegistryActor {

  final case class GeoPerformed(description: String)

  final case class CreateGeo(geo: UserGeo)

  final case class GetGeos(geo: GeoRequest)

  def props: Props = Props[GeoRegistryActor]
}

class GeoRegistryActor extends Actor with JsonSupport with ActorLogging {

  import GeoRegistryActor._

  val users = List.empty[User]
  private val redis = new RedisClient(sys.env("REDIS_URL"), sys.env("REDIS_PORT").toInt)

  def receive: Receive = {
    case GetGeos(geo) =>
      sender() ! Geos(redis.georadius("maps", geo.x, geo.y, geo.distance, "km", true, false, false, None, None, None, None)
        .get
        .flatten
        .map(el => redis.get(el.member.get).getOrElse("[]").parseJson.convertTo[UserGeo]))

    case CreateGeo(geo) =>
      val uuid = randomUUID()
      redis.set(uuid, geo.toJson.toString)
      redis.geoadd("maps", Seq((geo.x, geo.y, uuid)))
      redis.publish("mapsChannel", uuid.toString)
      sender() ! GeoPerformed(uuid.toString)

  }
}
