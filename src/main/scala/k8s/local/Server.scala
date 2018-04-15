package k8s.local

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import k8s.local.registry.{ ClusterMemberShipRegistry, GeoRegistryActor, UserRegistryActor }
import k8s.local.routes.{ ClusterRoutes, GeoRoutes, LoginRoutes, UserRoutes }

import scala.concurrent.duration._

object Server extends App with UserRoutes with GeoRoutes with LoginRoutes with ClusterRoutes {

  val actorSystemName = sys.env("AKKA_ACTOR_SYSTEM_NAME")

  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val mat = ActorMaterializer()
  import actorSystem.dispatcher
  implicit val http = Http(actorSystem)
  implicit val routingSettings = RoutingSettings(actorSystem)

  val clusterMembershipAskTimeout = FiniteDuration(sys.env("CLUSTER_MEMBERSHIP_ASK_TIMEOUT").toLong, TimeUnit.MILLISECONDS)

  val geoRegistryActor: ActorRef = actorSystem.actorOf(GeoRegistryActor.props, "geoRegistryActor")
  val userRegistryActor: ActorRef = actorSystem.actorOf(UserRegistryActor.props, "userRegistryActor")
  val clusterMemberShipRegistryActor: ActorRef = actorSystem.actorOf(ClusterMemberShipRegistry.props, "clusterRegistryActor")

  val userRoute: Route = userRoutes(userRegistryActor, clusterMembershipAskTimeout)
  val geoRoute: Route = geoRoutes(geoRegistryActor, clusterMembershipAskTimeout)
  val loginRoute: Route = loginRoutes(userRegistryActor, clusterMembershipAskTimeout)
  val membersRoute: Route = clusterRoutes(clusterMemberShipRegistryActor, clusterMembershipAskTimeout)

  lazy val routes: Route = userRoute ~ geoRoute ~ loginRoute ~ membersRoute

  http.bindAndHandle(routes, sys.env("HTTP_HOST"), sys.env("HTTP_PORT").toInt)

  println(s"Server online at http://" + sys.env("HTTP_HOST") + ":" + sys.env("HTTP_PORT").toInt + "/")

}
