package k8s.local.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import k8s.local.dto.{GeoRequest, UserGeo}
import k8s.local.registry.GeoRegistryActor._
import k8s.local.registry.Geos
import k8s.local.tools.{Authentication, JsonSupport}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait GeoRoutes extends JsonSupport {
  def geoRoutes(geoRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =
    Route.seal(
      cors() {
        pathPrefix("geo") {
          concat(
            pathEnd {
              concat(
                post {
                  Authentication.authenticated { _ =>
                    entity(as[UserGeo]) { geo =>
                      val geoCreated: Future[GeoPerformed] =
                        geoRegistryActor.ask(CreateGeo(geo))(askTimeout).mapTo[GeoPerformed]
                      onSuccess(geoCreated) { performed =>
                        complete((StatusCodes.Created, performed))
                      }
                    }
                  }
                },
                get {
                  Authentication.authenticated { _ =>
                    parameters('x, 'y, 'distance) { (x, y, distance) =>
                      val geos =
                        geoRegistryActor.ask(GetGeos(GeoRequest(x.toFloat, y.toFloat, distance.toInt)))(askTimeout).mapTo[Geos]
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
    )

}