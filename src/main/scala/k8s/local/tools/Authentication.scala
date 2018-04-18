package k8s.local.tools

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{ optionalHeaderValueByName, provide }
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import authentikat.jwt.{ JsonWebToken, JwtClaimsSet, JwtHeader }

final case class LoginRequest(username: String, password: String)

object Authentication {

  val secretKey = sys.env("JWT_TOKEN")
  val header = JwtHeader("HS256")

  def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired.")

      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(getClaims(jwt).getOrElse(Map.empty[String, Any]))

      case _ => complete(StatusCodes.Unauthorized)
    }

  def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get("expiredAt") match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }

  def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }

  def setClaims(username: String, expiryPeriodInDays: Long) = JwtClaimsSet(
    Map(
      "user" -> username,
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS
        .toMillis(expiryPeriodInDays))
    )
  )

}
