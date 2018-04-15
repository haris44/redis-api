package k8s.local.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.ActorMaterializer
import authentikat.jwt.JsonWebToken
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import k8s.local.registry.User
import k8s.local.registry.UserRegistryActor.GetUser
import k8s.local.tools.{Authentication, JsonSupport, LoginRequest}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait LoginRoutes extends JsonSupport {
  private val tokenExpiryPeriodInDays = 1

  def loginRoutes(userRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =
    Route.seal(
      cors() {
        pathPrefix("login") {
          pathEnd {
            concat(
              post {
                entity(as[LoginRequest]) { lr =>
                  val user = userRegistryActor.ask(GetUser(lr.username))(askTimeout).mapTo[User]
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
    )
}
