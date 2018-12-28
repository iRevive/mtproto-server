package com.mtproto.server.protocol.auth.s2c

import scodec._
import scodec.bits._
import scodec.codecs._

final case class PQResponse(
    constructorId: Int,
    nonce: Vector[Byte],
    serverNonce: Vector[Byte],
    pq: Vector[Byte],
    vecConstructorId: Int,
    fingerprints: Vector[Long]
)

object PQResponse {

  val ConstructorId: Int = hex"05162463".toInt()

  implicit val codec: Codec[PQResponse] =
    (("constructor_id" | int32) ::
      ("nonce" | vectorOfN(provide(16), byte)) ::
      ("server_nonce" | vectorOfN(provide(16), byte)) ::
      ("pq" | paddedFixedSizeBytes(12, vectorOfN(int8, byte), constant(ByteVector.fromByte(0)))) ::
      ("vec_constructor_id" | int32) ::
      ("fingerprints" | vectorOfN(int32, int64))).as[PQResponse]

}
