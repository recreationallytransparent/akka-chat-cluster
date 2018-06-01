package com.pickard.chat.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck, Unsubscribe}
import com.pickard.chat.actors.ChatClient.{AssignWebSocket}
import com.pickard.chat.cluster.ClusterChatEvents
import com.pickard.chat.protocol.ChatProtocol

object ChatClient {
  def apply(id: String) = Props(new ChatClient(id))

  case class AssignWebSocket(ref: ActorRef)
  case class Publish(message: ChatProtocol.ChatRoomEvent)
  case class Complete()

  case class Consumed(message: ChatProtocol.ChatEvent)
}
/**
  * Created by lenovo on 5/31/2018.
  */
class ChatClient(id: String) extends Actor with ActorLogging {
  protected val mediator: ActorRef = DistributedPubSub(context.system).mediator

  protected var websocket: Option[ActorRef] = None
  private val subscriptions = scala.collection.mutable.Set.empty[String]

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"ChatClient-$id running...")
    mediator ! Subscribe(ClusterChatEvents.TOPIC, self)
  }

  override def receive: Receive = {
    case AssignWebSocket(ref) =>
      log.info(s"ChatClient-$id - New master: $ref")
      mediator ! Publish(ClusterChatEvents.TOPIC, ChatProtocol.UserSignOn(id, System.currentTimeMillis()))
      websocket = Some(ref)
    // receives from userSocket, must handle join differently because we have to alery the system
    case ChatClient.Publish(j: ChatProtocol.Join) =>
      log.info(s"ChatClient-$id attempting to subscribe to ${j.chatId}")
      mediator ! Publish(ClusterChatEvents.TOPIC, j)
      subscriptions.filterNot(_ == ClusterChatEvents.TOPIC).foreach(x => {
        mediator ! Unsubscribe(x, self)
        // publish a departing event to the chat we are leaving
        log.info(s"ChatClient-$id moving from $x to ${j.chatId}")
        mediator ! Publish(x, ChatProtocol.Left(id, x, System.currentTimeMillis()))
      })
      mediator ! Subscribe(j.chatId, self)
    // receives from userSocket
    case ChatClient.Publish(event) =>
      log.info(s"ChatClient-$id publishing $event to chat: ${event.chatId}")
      mediator ! Publish(event.chatId, event)
    // Receives chatevents from bus
    case e: ChatProtocol.ChatRoomEvent =>
      websocket.foreach(_ ! ChatClient.Consumed(e))
    case s: ChatProtocol.ChatState =>
      websocket.foreach(_ ! ChatClient.Consumed(s))
    case c: ChatClient.Complete =>
      log.info(s"ChatClient-$id Received complete command... shutting down ChatClient-$id")
      subscriptions.foreach(s => mediator ! Unsubscribe(s, self))
      mediator ! Publish(ClusterChatEvents.TOPIC, ChatProtocol.UserSignOff(id, System.currentTimeMillis()))
      context.stop(self)
    case a: SubscribeAck =>
      log.info(s"ChatClient-$id received SubscribeAck: ${a.subscribe.topic}")
      subscriptions.add(a.subscribe.topic)
  }
}
