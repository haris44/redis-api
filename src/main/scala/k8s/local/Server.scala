package k8s.local

//#quick-start-server
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.http.scaladsl.server.Directives._
import k8s.local.registry.{GeoRegistryActor, UserRegistryActor}
import k8s.local.routes.{GeoRoutes, LoginRoutes, UserRoutes}

//#main-class
object Server extends App with UserRoutes with GeoRoutes with LoginRoutes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping

  val actorSystemName = sys.env("AKKA_ACTOR_SYSTEM_NAME")
  implicit val system: ActorSystem = ActorSystem(actorSystemName)

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //#server-bootstrapping

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")
  val geoRegistryActor: ActorRef = system.actorOf(GeoRegistryActor.props, "geoRegistryActor")

  //#main-class
  // from the UserRoutes trait
  lazy val routes: Route = userRoutes ~ geoRoutes ~ loginRoutes
  //#main-class

  //#http-server
  Http().bindAndHandle(routes, "0.0.0.0", 8080)

  println(s"Server online at http://0.0.0.0:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
//#quick-start-server
