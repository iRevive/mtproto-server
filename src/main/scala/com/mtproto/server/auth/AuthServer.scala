package com.mtproto.server.auth

import java.nio.ByteOrder

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import scodec.bits._

import scala.util.control.NonFatal

object AuthServer extends StrictLogging {

  private val authHandler = new AuthHandler(ClientStorage.inMemoryClientStorage())

  def handler: Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString]
      .via(packetFraming)
      .map(packet => authHandler.handle(ByteVector(packet.toArray)).fold(e => throw e, r => ByteString(r.toByteBuffer)))
      .recover {
        case NonFatal(ProcessingError(cause)) =>
          logger.error(s"Processing completed with error $cause")
          ByteString(InvalidRequestAnswer.toByteBuffer)

        case NonFatal(e) =>
          logger.error(s"Processing completed with error ${e.getMessage}", e)
          ByteString(InvalidRequestAnswer.toByteBuffer)
      }

  private def packetFraming: Flow[ByteString, ByteString, NotUsed] =
    Framing.lengthField(4, 16, Int.MaxValue - 20, ByteOrder.BIG_ENDIAN)

  private val InvalidRequestAnswer: ByteVector = hex"FFFFFFFF"

}
