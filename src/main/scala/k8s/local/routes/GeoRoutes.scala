package k8s.local.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import k8s.local.dto.{ GeoRequest, UserGeo }
import k8s.local.registry.GeoRegistryActor._
import k8s.local.registry.Geos
import k8s.local.tools.{ Authentication, JsonSupport }

import scala.concurrent.Future
import scala.concurrent.duration._

trait GeoRoutes extends JsonSupport {

  implicit def system: ActorSystem

  def geoRegistryActor: ActorRef

  lazy val geoLog = Logging(system, classOf[GeoRoutes])
  implicit lazy val geoTimeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val geoRoutes: Route = cors() {
    pathPrefix("geo") {
      concat(
        pathEnd {
          concat(
            post {
              Authentication.authenticated { _ =>
                entity(as[UserGeo]) { geo =>
                  val geoCreated: Future[GeoPerformed] =
                    (geoRegistryActor ? CreateGeo(geo)).mapTo[GeoPerformed]
                  onSuccess(geoCreated) { performed =>
                    geoLog.info("Created geo [{}]: {}", geo.username, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
                }
              }
            },
            get {
              Authentication.authenticated { _ =>
                parameters('x, 'y, 'distance) { (x, y, distance) =>
                  val geos =
                    (geoRegistryActor ? GetGeos(GeoRequest(x.toFloat, y.toFloat, distance.toInt))).mapTo[Geos]
                  rejectEmptyResponse {
                    complete(geos)
                  }
                }
              }
            }
          )
        }
      )
    }
  }

}