package com.pickard.chat

import akka.actor.{ActorSystem, Address}
import akka.cluster.Cluster

/**
  * Created by lenovo on 6/1/2018.
  */
object AnotherOne {
  def main(args: Array[String]): Unit = {
//    val address = args(1)
    val systemName = "PickardAkkaClusterChat"

    val system = ActorSystem(systemName)
    val cluster = Cluster(system)
    val clusterAddress = cluster.selfAddress

    println("Attempting to attach to cluster")
//    Cluster(system).join(address = Address("akka", systemName))
    cluster.join(clusterAddress)
    println(cluster.state)
  }
}
