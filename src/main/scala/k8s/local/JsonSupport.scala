package k8s.local

import k8s.local.GeoRegistryActor.GeoPerformed
import k8s.local.UserRegistryActor.ActionPerformed

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat2(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val geoJsonFormat = jsonFormat3(Geo)
  implicit val geosJsonFormat = jsonFormat1(Geos)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
  implicit val geoPerformedJsonFormat = jsonFormat1(GeoPerformed)

}
//#json-support
