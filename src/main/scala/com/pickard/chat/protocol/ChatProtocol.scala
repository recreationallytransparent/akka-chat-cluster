package com.pickard.chat.protocol

import org.json4s.{DateFormat, DefaultFormats, Formats, ShortTypeHints}

import scala.util.{Failure, Success, Try}

/**
  * Define protocol and other related stuff to communication between client and web socket server
  */
object ChatProtocol {
  import org.json4s.jackson.Serialization

  sealed trait ChatEvent {
    val time: Long
  }

  sealed trait UserEvent extends ChatEvent {
    val senderId: String
  }

  sealed trait ChatRoomEvent extends UserEvent {
    val chatId: String
  }

  case class UserSignOn(senderId: String, time: Long) extends UserEvent
  case class UserSignOff(senderId: String, time: Long) extends UserEvent

  case class Message(senderId: String, chatId: String, contents: String, time: Long) extends ChatRoomEvent
  case class Join(senderId: String, chatId: String, time: Long) extends ChatRoomEvent
  case class Left(senderId: String, chatId: String, time: Long) extends ChatRoomEvent
  case class Typing(senderId: String, chatId: String, time: Long) extends ChatRoomEvent

  case class ChatState(rooms: Set[String], users: Set[String], roomMembership: Map[String, Set[String]], time: Long) extends ChatEvent

  implicit protected val formats = new Formats {
    override def dateFormat: DateFormat = DefaultFormats.lossless.dateFormat
    override def typeHints = ShortTypeHints(List(
      classOf[Message],
      classOf[Join],
      classOf[Left],
      classOf[Typing],
      classOf[ChatState]
    ))
  }

  def deserialize(s: String): ChatEvent = {
//    Try(Serialization.read[ChatEvent](s)) match {
//      case Success(e) => e
//      case Failure(t) =>
//        t.printStackTrace()
//        throw t
//    }
    Serialization.read[ChatEvent](s)
  }

  def serialize(e: ChatEvent): String = Serialization.write(e)
}

