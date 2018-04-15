package k8s.local.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import k8s.local.registry.User
import k8s.local.registry.UserRegistryActor._
import k8s.local.tools.{Authentication, JsonSupport}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait UserRoutes extends JsonSupport {

  implicit lazy val userTimeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  def userRoutes(userRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =
    Route.seal(
      cors() {
        pathPrefix("users") {
          concat(
            pathEnd {
              concat(
                post {
                  entity(as[User]) { user =>
                    val userCreated: Future[ActionPerformed] =
                      (userRegistryActor ? CreateUser(user.copy(password = BCrypt.hashpw(user.password, BCrypt.gensalt)))).mapTo[ActionPerformed]
                    onSuccess(userCreated) { performed =>
                      complete((StatusCodes.Created, performed))
                    }
                  }
                }
              )
            },
            path(Segment) { name =>
              concat(
                get {
                  Authentication.authenticated { _ =>
                    val maybeUser: Future[Option[User]] =
                      (userRegistryActor ? GetUser(name)).mapTo[Option[User]]
                    rejectEmptyResponse {
                      complete(maybeUser)
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
