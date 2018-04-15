package k8s.local.routes

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
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import k8s.local.registry.User
import k8s.local.registry.UserRegistryActor._
import k8s.local.tools.{ Authentication, JsonSupport }
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future
import scala.concurrent.duration._

trait UserRoutes extends JsonSupport {

  implicit def system: ActorSystem

  def userRegistryActor: ActorRef

  lazy val userLog = Logging(system, classOf[UserRoutes])
  implicit lazy val userTimeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val userRoutes: Route = cors() {
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[User]) { user =>
                val userCreated: Future[ActionPerformed] =
                  (userRegistryActor ? CreateUser(user.copy(password = BCrypt.hashpw(user.password, BCrypt.gensalt)))).mapTo[ActionPerformed]
                onSuccess(userCreated) { performed =>
                  userLog.info("Created user [{}]: {}", user.name, performed.description)
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
}
