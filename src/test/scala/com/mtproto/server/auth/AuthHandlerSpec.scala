package com.mtproto.server.auth

import cats.scalatest.{EitherMatchers, EitherValues}
import com.mtproto.server.protocol.UnencryptedPacket
import com.mtproto.server.protocol.auth.c2s.{DHRequest, PQRequest}
import com.mtproto.server.protocol.auth.s2c.PQResponse
import com.mtproto.server.protocol.cipher.CipherUtils
import org.scalatest.{Matchers, WordSpec}
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

import scala.util.Random

class AuthHandlerSpec extends WordSpec with Matchers with EitherMatchers with EitherValues {

  "AuthHandler" should {

    "return an error" when {

      "input byte vector has invalid structure" in {
        val handler = new AuthHandler(ClientStorage.inMemoryClientStorage())
        val result  = handler.handle(ByteVector.empty)

        result should beLeft(ProcessingError("auth_key_id: cannot acquire 64 bits from a vector that contains 0 bits"))
      }

      "auth key id is not zero" in {
        val handler = new AuthHandler(ClientStorage.inMemoryClientStorage())
        val bytes   = Codec.encode(UnencryptedPacket(1L, 0, ByteVector.empty)).toEither.value
        val result  = handler.handle(bytes.toByteVector)

        result should beLeft(ProcessingError("auth_key_id should be 0 in case of unencrypted packet"))
      }

      "packet is unknown" in {
        val handler = new AuthHandler(ClientStorage.inMemoryClientStorage())
        val bytes   = Codec.encode(UnencryptedPacket(0L, 0, int32.encode(123).toEither.value.toByteVector)).toEither.value
        val result  = handler.handle(bytes.toByteVector)

        result should beLeft(ProcessingError("Cannot find packet reader for constructor_id [123]"))
      }

    }

    "return a correct response" when {

      "pq request submitted" in {
        val handler = new AuthHandler(ClientStorage.inMemoryClientStorage())

        val nonce   = randomByteVector(16)
        val request = PQRequest(nonce)

        val constructorId  = int32.encode(PQRequest.ConstructorId).toEither.value
        val encodedRequest = Codec.encode(request).toEither.value
        val requestBytes   = (constructorId ++ encodedRequest).toByteVector

        val bytes = Codec.encode(UnencryptedPacket(0L, 0, requestBytes)).toEither.value

        val result  = handler.handle(bytes.toByteVector).value
        val decoded = Codec.decode[UnencryptedPacket](result).toEither.value.value
        val body    = Codec.decode[PQResponse](decoded.data.toBitVector).toEither.value.value

        decoded.authKeyId shouldBe 0L

        body.constructorId shouldBe PQResponse.ConstructorId
        body.nonce shouldBe nonce
        body.serverNonce.length shouldBe 16
        body.pq.length shouldBe 8
        body.fingerprints should contain only CipherUtils.little64bits.toLong()
      }

      "dh request submitted" in {
        implicit val cipherFactory: CipherFactory = CipherUtils.cipherFactory()

        val handler = new AuthHandler(ClientStorage.inMemoryClientStorage())

        val nonce   = randomByteVector(16)
        val request = PQRequest(nonce)

        val constructorId  = int32.encode(PQRequest.ConstructorId).toEither.value
        val encodedRequest = Codec.encode(request).toEither.value
        val requestBytes   = (constructorId ++ encodedRequest).toByteVector

        val pqRequestBytes = Codec.encode(UnencryptedPacket(0L, 0, requestBytes)).toEither.value

        val result     = handler.handle(pqRequestBytes.toByteVector).value
        val decoded    = Codec.decode[UnencryptedPacket](result).toEither.value.value
        val pqResponse = Codec.decode[PQResponse](decoded.data.toBitVector).toEither.value.value

        val dhRequest        = DHRequest(nonce, pqResponse.serverNonce, pqResponse.pq, pqResponse.fingerprints.head, Vector.empty)
        val dhConstructorId  = int32.encode(DHRequest.ConstructorId).toEither.value
        val dhEncodedRequest = Codec.encode(dhRequest).toEither.value
        val dhRequestBytes   = (dhConstructorId ++ dhEncodedRequest).toByteVector

        val dhRequestEncoded = Codec.encode(UnencryptedPacket(0L, 0, dhRequestBytes)).toEither.value
        val dhResult         = handler.handle(dhRequestEncoded.toByteVector).value
        val dhDecoded        = Codec.decode[UnencryptedPacket](dhResult).toEither.value.value

        dhDecoded shouldBe UnencryptedPacket(0L, 0L, ByteVector.empty)
      }

    }

  }

  private def randomByteVector(length: Int): Vector[Byte] = {
    val bytesArray = new Array[Byte](length)
    Random.nextBytes(bytesArray)
    bytesArray.toVector
  }

}
