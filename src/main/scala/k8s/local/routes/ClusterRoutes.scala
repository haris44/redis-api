package k8s.local.routes

import akka.actor.ActorRef
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.ActorMaterializer
import akka.pattern._
import k8s.local.dto.{ KubernetesMember, KubernetesMembers }
import k8s.local.registry.ClusterMemberShipRegistry
import k8s.local.tools.JsonSupport

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.FiniteDuration
import Directives._

trait ClusterRoutes extends JsonSupport {
  def clusterRoutes(clusterMembership: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route = {
    pathPrefix("cluster") {
      pathEnd {
        get {
          val ft: Future[ClusterMemberShipRegistry.MembershipInfo] = clusterMembership.ask(ClusterMemberShipRegistry.GetMembershipInfo)(askTimeout)
            .mapTo[ClusterMemberShipRegistry.MembershipInfo]

          onSuccess(ft) { performed =>
            val res = performed.members.map(el => KubernetesMember(el.uniqueAddress.address.toString, el.status.toString)).toList
            complete(KubernetesMembers(res))
          }
        }
      }
    }
  }
}