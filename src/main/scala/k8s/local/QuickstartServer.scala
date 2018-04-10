package k8s.local

//#quick-start-server
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.http.scaladsl.server.Directives._

//#main-class
object QuickstartServer extends App with UserRoutes with GeoRoutes {

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
  lazy val routes: Route = userRoutes ~ geoRoutes
  //#main-class

  //#http-server
  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
//#quick-start-server
