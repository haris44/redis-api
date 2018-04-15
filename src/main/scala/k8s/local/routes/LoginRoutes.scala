package k8s.local.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import authentikat.jwt.JsonWebToken
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import k8s.local.registry.User
import k8s.local.registry.UserRegistryActor.GetUser
import k8s.local.tools.{ Authentication, JsonSupport, LoginRequest }
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.duration._

trait LoginRoutes extends JsonSupport {

  implicit def system: ActorSystem

  def userRegistryActor: ActorRef

  private val tokenExpiryPeriodInDays = 1
  lazy val loginLog = Logging(system, classOf[LoginRoutes])

  implicit lazy val loginTimeout: Timeout = Timeout(5.seconds)

  lazy val loginRoutes: Route = cors() {
    pathPrefix("login") {
      pathEnd {
        concat(
          post {
            entity(as[LoginRequest]) { lr =>
              val user = (userRegistryActor ? GetUser(lr.username)).mapTo[User]
              onSuccess(user) { usr =>
                if (lr.username == usr.username && BCrypt.checkpw(lr.password, usr.password)) {
                  val claims = Authentication.setClaims(lr.username, tokenExpiryPeriodInDays)
                  respondWithHeader(RawHeader("Access-Token", JsonWebToken(Authentication.header, claims, Authentication.secretKey))) {
                    complete(StatusCodes.OK)
                  }
                } else {
                  complete(StatusCodes.Unauthorized)
                }
              }
            }
          }, get {
            Authentication.authenticated { claims =>
              complete(s"User ${claims.getOrElse("user", "")} accessed secured content!")
            }
          }
        )
      }
    }
  }
}
