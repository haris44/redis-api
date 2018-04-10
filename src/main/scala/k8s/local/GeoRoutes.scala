package k8s.local

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import k8s.local.GeoRegistryActor._

import scala.concurrent.Future
import scala.concurrent.duration._

trait GeoRoutes extends JsonSupport {

  implicit def system: ActorSystem
  def geoRegistryActor: ActorRef

  lazy val geoLog = Logging(system, classOf[GeoRoutes])
  implicit lazy val geoTimeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val geoRoutes: Route =
    pathPrefix("geo") {
      concat(
        pathEnd {
          concat(
            get {
              val users: Future[Geos] =
                (geoRegistryActor ? GetGeos).mapTo[Geos]
              complete(users)
            },
            post {
              entity(as[Geo]) { geo =>
                val geoCreated: Future[GeoPerformed] =
                  (geoRegistryActor ? CreateGeo(geo)).mapTo[GeoPerformed]
                onSuccess(geoCreated) { performed =>
                  geoLog.info("Created geo [{}]: {}", geo.name, performed.description)
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        path(Segment) { name =>
          concat(
            get {
              val maybeUser: Future[Option[User]] =
                (geoRegistryActor ? GetGeo(name)).mapTo[Option[User]]
              rejectEmptyResponse {
                complete(maybeUser)
              }
            }
          )
        }
      )
    }
}