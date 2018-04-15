package k8s.local.tools

import k8s.local.dto.{KubernetesMember, KubernetesMembers, UserGeo}
import k8s.local.registry.GeoRegistryActor.GeoPerformed
import k8s.local.registry.UserRegistryActor.ActionPerformed
import k8s.local.registry.{Geo, Geos, User, Users}

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val kubernetesMemberJsonFormat = jsonFormat2(KubernetesMember)
  implicit val kubernetesMembersJsonFormat = jsonFormat1(KubernetesMembers)
  implicit val userGeoJsonFormat = jsonFormat4(UserGeo)

  implicit val geoJsonFormat = jsonFormat3(Geo)
  implicit val geosJsonFormat = jsonFormat1(Geos)

  implicit val loginRequestJsonFormat = jsonFormat2(LoginRequest)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
  implicit val geoPerformedJsonFormat = jsonFormat1(GeoPerformed)

}
//#json-support
