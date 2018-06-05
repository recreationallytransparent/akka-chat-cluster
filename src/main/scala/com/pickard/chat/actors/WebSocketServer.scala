package com.pickard.chat.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessages, parameter, path}
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.stream.javadsl.GraphDSL
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import com.pickard.chat.cluster.ClusterChatEvents
import com.pickard.chat.protocol.ChatProtocol
import org.json4s.MappingException

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object WebSocketServer {
  def apply()(implicit materializer: ActorMaterializer) = Props(new WebSocketServer)
  case class Start(host: String, port: Int)
  case class Stop()
}
/**
  * Created by lenovo on 6/1/2018.
  */
class WebSocketServer(implicit materializer: ActorMaterializer) extends Actor with ActorLogging {
  import WebSocketServer._

  private implicit val system = context.system
  protected var httpBinding: Http.ServerBinding = null

  override def receive: Receive = {
    case Start(host, port) =>
      val s = sender()
      Http()(context.system).bindAndHandle(RouteResult.route2HandlerFlow(route), host, port)
        .onComplete {
          case Success(binding) =>
            val localAddress = binding.localAddress
            log.info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
            httpBinding = binding
            s ! Start(binding.localAddress.getHostName, binding.localAddress.getPort)
          case Failure(e) =>
            log.error("Failed to start")
            log.error(e, e.getMessage)
            httpBinding = null
            s ! Stop()
        }
    case Stop() =>
      if (httpBinding != null) {
        log.info("Stopping on command")
        httpBinding.unbind()
        httpBinding = null
      }
      sender() ! Stop()
      context.stop(self)
  }

  def route: Route =
    get {
      path ("chat") {
        parameter('id) { id => handleWebSocketMessages(handle(id))}
      }
    }

  def handle(userId: String): Flow[Message, Message, Any] = {
    val chatClient: ActorRef = system.actorOf(ChatClient(userId))

    val source = Source.actorRef[ChatClient.Consumed](10, OverflowStrategy.fail)
        .mapMaterializedValue(ref => {
          chatClient ! ChatClient.AssignWebSocket(ref)
          NotUsed
        })
      .map(consumed => TextMessage(ChatProtocol.serialize(consumed.message)))

    val sink = Flow[Message].collect {
      case TextMessage.Strict(msg: String) => Future.successful(msg)
      case TextMessage.Streamed(textStream) => textStream.runFold("")(_ + _).flatMap(Future.successful)
    }
      .mapAsync(1)(identity)
      .map(ChatProtocol.deserialize)
      .mapError({
        case e: MappingException =>
          e.printStackTrace()
          log.error(e, "Error deserializing")
          e
      })
      .collect({
        case m: ChatProtocol.ChatRoomEvent => ChatClient.Publish(m)
      })
      .to(Sink.actorRef(chatClient, ChatClient.Complete()))

    Flow.fromSinkAndSource(sink, source)
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) => log.error(s"failed with cause: $cause", cause)
        case _ =>
      })
  }
}
