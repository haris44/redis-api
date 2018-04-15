package k8s.local

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.http.scaladsl.server.Directives._
import k8s.local.registry.{GeoRegistryActor, UserRegistryActor}
import k8s.local.routes.{GeoRoutes, LoginRoutes, UserRoutes}

object Server extends App with UserRoutes with GeoRoutes with LoginRoutes {


  val actorSystemName = sys.env("AKKA_ACTOR_SYSTEM_NAME")
  implicit val system: ActorSystem = ActorSystem(actorSystemName)

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")
  val geoRegistryActor: ActorRef = system.actorOf(GeoRegistryActor.props, "geoRegistryActor")

  lazy val routes: Route = userRoutes ~ geoRoutes ~ loginRoutes

  Http().bindAndHandle(routes, sys.env("HTTP_HOST"), sys.env("HTTP_PORT").toInt)

  println(s"Server online at http://" + sys.env("HTTP_HOST") + ":" + sys.env("HTTP_PORT").toInt + "/")

  Await.result(system.whenTerminated, Duration.Inf)

}
