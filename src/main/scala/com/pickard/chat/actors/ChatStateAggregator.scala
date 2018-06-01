package com.pickard.chat.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.stream.{ActorMaterializer, Materializer}
import com.pickard.chat.protocol.ChatProtocol
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by lenovo on 6/1/2018.
  */
object ChatStateAggregator {
  def apply()(implicit materializer: ActorMaterializer) = Props(new ChatStateAggregator)

  case class Tick()
}

class ChatStateAggregator(implicit val materializer: ActorMaterializer) extends Actor with ActorLogging {
  import com.pickard.chat.cluster.ClusterChatEvents

  protected val bus: ActorRef = DistributedPubSub(context.system).mediator

  // maintain a set of ids over the lifetime of this actor
  // in production you can do something like populate this with a store
  private val chatRoomIds = scala.collection.mutable.Set.empty[String]
  private val userIds = scala.collection.mutable.Set.empty[String]
  private val roomMembership = scala.collection.mutable.Map.empty[String, String]

  private def state: ChatProtocol.ChatState =  ChatProtocol.ChatState(
    users = userIds.toSet,
    rooms = chatRoomIds.toSet,
    roomMembership = roomMembership.groupBy(_._2).mapValues(_.keys.toSet),
    time = System.currentTimeMillis()
  )

  override def preStart(): Unit = {
    log.info("Subscribing")

    super.preStart()
    // We are going to aggregate events that happen on the cluster
    bus ! Subscribe(ClusterChatEvents.TOPIC, self)

    // Every 5 seconds publish latest state of chatids
    context.system.scheduler.schedule(0.seconds, 5.seconds, self, ChatStateAggregator.Tick())
  }


  def updateState(message: ChatProtocol.Join): ChatProtocol.ChatState = {
    val newRoom = chatRoomIds.add(message.chatId)
    val newUser = userIds.add(message.senderId)
    val userMoved: Option[String] = roomMembership.put(message.senderId, message.chatId)

    if (newRoom) {
      log.info(s"ChatStateAggregator - added room ${message.chatId} there are ${chatRoomIds.size} chat rooms")
    }

    if (newUser) {
      log.info(s"ChatStateAggregator - added user ${message.senderId} there are ${userIds.size} uses")
    }

    if (userMoved.isDefined) {
      log.info(s"Detecteed user ${message.senderId} moved from ${userMoved.get} to ${message.chatId}")
    }

    state
  }

  def updateState(message: ChatProtocol.UserSignOn): ChatProtocol.ChatState = {
    if (userIds.add(message.senderId)) {
      log.info(s"ChatStateAggregator - new user: ${message.senderId}")
    }
    state
  }

  def updateState(message: ChatProtocol.UserSignOff): ChatProtocol.ChatState = {
    roomMembership.remove(message.senderId)
    if (userIds.remove(message.senderId)) {
      log.info(s"ChatStateAggregator - user left: ${message.senderId}")
    }
    state
  }

  override def receive: Receive = {
    case x: ChatProtocol.Join =>
      bus ! Publish(ClusterChatEvents.TOPIC, updateState(x))
    case l: ChatProtocol.UserSignOn =>
      bus ! Publish(ClusterChatEvents.TOPIC, updateState(l))
    case l: ChatProtocol.UserSignOff =>
      bus ! Publish(ClusterChatEvents.TOPIC, updateState(l))
    case x: ChatStateAggregator.Tick =>
      bus ! Publish(ClusterChatEvents.TOPIC, state)
  }
}
