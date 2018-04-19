package k8s.local.registry

import akka.actor.{ Actor, Props }
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.{ Cluster, ClusterEvent, Member }

object ClusterMemberShipRegistry {
  val Name = "cluster-membership"

  def props: Props = Props(new ClusterMemberShipRegistry)

  sealed trait Message

  /**
   * Sent as a request to obtain membership info
   */
  case object GetMembershipInfo extends Message

  /**
   * Sent as a reply to [[GetMembershipInfo]]; contains the list of [[members]] of the cluster.
   */
  case class MembershipInfo(members: Set[Member]) extends Message
}

abstract class ClusterMembershipAware extends Actor {

  override def receive = handleMembershipEvents(Set.empty)

  protected def handleMembershipEvents(members: Set[Member]): Receive = {
    case event: ClusterEvent.MemberRemoved =>
      context.become(handleMembershipEvents(members.filterNot(_ == event.member)))

    case event: ClusterEvent.MemberEvent =>
      context.become(handleMembershipEvents(members.filterNot(_ == event.member) + event.member))

    case ClusterMemberShipRegistry.GetMembershipInfo =>
      sender() ! ClusterMemberShipRegistry.MembershipInfo(members)
  }
}

/**
 * Subscribes to the membership events, stores the updated list of the members in the Akka cluster.
 */
class ClusterMemberShipRegistry extends ClusterMembershipAware {
  private val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = ClusterEvent.InitialStateAsEvents, classOf[MemberEvent])

  override def postStop(): Unit =
    cluster.unsubscribe(self)
}