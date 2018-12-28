package com.mtproto.server.auth

import cats.instances.long.catsKernelStdOrderForLong
import cats.syntax.either._
import cats.syntax.eq._
import com.mtproto.server.protocol.auth.c2s.{ClientRequest, DHRequest, PQRequest}
import com.mtproto.server.protocol.auth.s2c.PQResponse
import com.mtproto.server.protocol.auth.UnencryptedAuthKey
import com.mtproto.server.protocol.cipher.CipherUtils
import com.mtproto.server.protocol.{Constructors, UnencryptedPacket}
import com.typesafe.scalalogging.StrictLogging
import scodec._
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{CipherFactory, int32}

import scala.util.Random

class AuthHandler(clientStorage: ClientStorage) extends StrictLogging {

  private implicit val cipherFactory: CipherFactory = CipherUtils.cipherFactory()

  private val packets: Map[Int, Codec[_ <: ClientRequest]] = Map(
    PQRequest.ConstructorId -> PQRequest.pqRequestCodec,
    DHRequest.ConstructorId -> DHRequest.dhRequestCodec
  )

  def handle(body: ByteVector): Either[ProcessingError, BitVector] = {
    for {
      req    <- readRequest(body)
      packet <- readPacket(req.data)
      _ = logger.info(s"Parsed packet [$packet]")
      result <- handlePacket(packet)
    } yield result
  }

  private def handlePacket[A](packet: ClientRequest): Either[ProcessingError, BitVector] = {
    packet match {
      case PQRequest(nonce) =>
        val serverNonce = randomByteVector(16)
        val pq          = randomByteVector(8)

        val response = PQResponse(
          constructorId = PQResponse.ConstructorId,
          nonce = nonce,
          serverNonce = serverNonce,
          pq = pq,
          vecConstructorId = Constructors.VectorLongConstructor,
          fingerprints = Vector(CipherUtils.little64bits.toLong())
        )

        clientStorage.put(Client(nonce, serverNonce, pq))

        for {
          responseEncoded <- encode[PQResponse](response)
          packet = UnencryptedPacket(0L, generateMessageId, responseEncoded.toByteVector)
          encodedPacket <- encode[UnencryptedPacket](packet)
        } yield encodedPacket

      case DHRequest(nonce, serverNonce, pq, _, _) =>
        @inline def validate[B](name: String, clientField: B, requestField: B): Either[ProcessingError, Unit] =
          Either.cond(
            test = clientField == requestField,
            right = (),
            left = ProcessingError(s"User has an invalid $name [$requestField]. Expected [$clientField]")
          )

        for {
          client   <- clientStorage.find(nonce).toRight(ProcessingError(s"User with nonce [$nonce] doesn't exist"))
          _        <- validate("nonce", client.nonce, nonce)
          _        <- validate("server nonce", client.serverNonce, serverNonce)
          _        <- validate("PQ vector", client.pq, pq)
          response <- encode[UnencryptedPacket](UnencryptedPacket(0L, 0L, ByteVector.empty))
        } yield response

      case other =>
        Left(ProcessingError(s"Unknown packet $other"))

    }
  }

  private def readRequest(body: ByteVector): Either[ProcessingError, UnencryptedPacket] = {
    for {
      packet <- decode[UnencryptedPacket](body)
      _ <- Either.cond(
        test = packet.authKeyId === UnencryptedAuthKey,
        right = (),
        left = ProcessingError("auth_key_id should be 0 in case of unencrypted packet")
      )
    } yield packet
  }

  private def readPacket(data: ByteVector): Either[ProcessingError, ClientRequest] = {
    for {
      constructorId <- decode[Int](data.take(4))(int32)
      packetCodec <- packets
        .get(constructorId)
        .toRight(ProcessingError(s"Cannot find packet reader for constructor_id [$constructorId]"))
      packet <- decode(data.drop(4))(packetCodec)
    } yield packet
  }

  private def decode[A: Codec](bytes: ByteVector): Either[ProcessingError, A] = {
    Codec[A].decodeValue(bytes.bits).toEither.leftMap(e => ProcessingError(e.messageWithContext))
  }

  private def encode[A: Codec](value: A): Either[ProcessingError, BitVector] = {
    Codec[A].encode(value).toEither.leftMap(e => ProcessingError(e.messageWithContext))
  }

  private def generateMessageId: Long = {
    (System.currentTimeMillis() / 1000L) * (2L << 31)
  }

  private def randomByteVector(length: Int): Vector[Byte] = {
    val bytesArray = new Array[Byte](length)
    Random.nextBytes(bytesArray)
    bytesArray.toVector
  }

}
