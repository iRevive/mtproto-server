package com.mtproto.server.protocol

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

final case class UnencryptedPacket(authKeyId: Long, messageId: Long, data: ByteVector)

object UnencryptedPacket {

  implicit val unencryptedRequestCodec: Codec[UnencryptedPacket] =
    (("auth_key_id" | int64) ::
      ("message_id" | int64) ::
      variableSizeBytes("message_data_length" | int32, "message_data" | bytes)).as[UnencryptedPacket]

}
