package com.mtproto.server.protocol

import java.nio.ByteBuffer

import org.scalatest.{Inside, Matchers, WordSpec}
import scodec.bits.ByteVector
import scodec.{Attempt, Codec, DecodeResult}

import scala.util.Random

class UnencryptedPacketSpec extends WordSpec with Matchers with Inside {

  "UnencryptedRequest" should {

    "be decoded from byte vector" in {
      val messageId   = Random.nextLong()
      val messageBody = Array.fill(10)(Random.nextInt(10).toByte)

      val buffer = ByteBuffer
        .allocate(30)
        .putLong(0L)
        .putLong(messageId)
        .putInt(messageBody.length)
        .put(messageBody)

      inside(Codec[UnencryptedPacket].decode(ByteVector(buffer).bits)) {
        case Attempt.Successful(DecodeResult(result, remainder)) =>
          result shouldBe UnencryptedPacket(0, messageId, ByteVector(messageBody))
          remainder.isEmpty shouldBe true
      }
    }

  }

}
