package com.pickard.chat

import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import com.pickard.chat.actors.{ChatStateAggregator, WebSocketServer}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

//object Main {
//  def main(args: Array[String]): Unit = {
//    val systemName = "PickardAkkaClusterChat"
//    implicit val system1 = ActorSystem(systemName)
//    implicit val materializer = ActorMaterializer()
//
//    val joinAddress = Cluster(system1).selfAddress
//
//    println(s"JOIN ADDRESS: $joinAddress")
//
//    Cluster(system1).join(joinAddress)
//    system1.actorOf(Props[MemberListener], "memberListener")
////    system1.actorOf(ChatStateAggregator())
////
////    val wsServer = system1.actorOf(WebSocketServer())
////
////    implicit val timeout: Timeout = 15.seconds
////    val x: Future[Any] = wsServer ? WebSocketServer.Start(joinAddress.host.getOrElse("localhost"), 8080)
////    x.onComplete({
////      case Success(s: WebSocketServer.Start) => println(s"Server running on ${s.host}:${s.port}")
////      case Success(s: WebSocketServer.Stop) =>
////        println(s"Everything is all fucked up")
////        System.exit(1)
////      case Failure(t) =>
////        t.printStackTrace()
////        println(s"Well that sucks: $t")
////    })
//  }
//}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      startup(args)
    } else {
      startup(Seq("2551", "2552"))
    }
  }

  def startup(ports: Seq[String]) = {
    val systemName = "PickardAkkaClusterChat"

    ports.map(port => {
      val config = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port=$port
        akka.remote.artery.canonical.port=$port
        """).withFallback(ConfigFactory.load())
      implicit val system = ActorSystem(systemName, config)
      val cluster = Cluster(system)
      implicit val materializer: ActorMaterializer = ActorMaterializer()

      system.actorOf(Props[MemberListener], name = "memberListener")

      val server = system.actorOf(WebSocketServer()(materializer), name = "websocketServer")
      implicit val timeout: akka.util.Timeout = 10.seconds
      val x = server ? WebSocketServer.Start(cluster.selfAddress.host.getOrElse("localhost"), port.toInt)

      x.onComplete({
        case Success(s: WebSocketServer.Start) => println(s"Server running on ${s.host}:${s.port}")
        case Success(s: WebSocketServer.Stop) =>
          println(s"Everything is all fucked up")
          System.exit(1)
        case Failure(t) =>
          t.printStackTrace()
          println(s"Well that sucks: $t")
          System.exit(1)
      })
      server
    })
  }
}