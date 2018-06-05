package com.pickard.chat

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Address
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus

class MemberListener extends Actor with ActorLogging {
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val cluster = Cluster(context.system)

  case class Tick()

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
    context.system.scheduler.schedule(0.seconds, 10.seconds, self, Tick)
  }

  override def postStop(): Unit =
    cluster unsubscribe self

  var nodes = Set.empty[Address]

  def receive = {
    case Tick =>
      log.info("Nodes in cluster: {}", nodes.size)
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.status == MemberStatus.Up => m.address
      }
    case MemberJoined(member) =>
      nodes += member.address
      log.info("Member joined: {}, nodes in cluster: {}", member.address, nodes.size)
    case MemberUp(member) =>
      nodes += member.address
      log.info("Member is Up: {}. {} nodes in cluster", member.address, nodes.size)
    case MemberRemoved(member, _) =>
      nodes -= member.address
      log.info("Member is Removed: {}. {} nodes cluster", member.address, nodes.size)
    case x: MemberEvent =>
      log.info(s"Cluster info member event: $x")
  }
}