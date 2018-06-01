package com.pickard.chat

import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import com.pickard.chat.actors.{ChatStateAggregator, WebSocketServer}
object Main {

  def main(args: Array[String]): Unit = {
    val systemName = "PickardAkkaClusterChat"
    implicit val system1 = ActorSystem(systemName)
    implicit val materializer = ActorMaterializer()

    val joinAddress = Cluster(system1).selfAddress

    Cluster(system1).join(joinAddress)
    system1.actorOf(Props[MemberListener], "memberListener")
    system1.actorOf(ChatStateAggregator())

    val wsServer = system1.actorOf(WebSocketServer())

    wsServer ! WebSocketServer.Start(joinAddress.host.getOrElse("localhost"), 8080)
  }
}
