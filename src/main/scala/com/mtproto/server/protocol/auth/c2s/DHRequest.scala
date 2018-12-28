package com.mtproto.server.protocol.auth
package c2s

import scodec._
import scodec.bits._
import scodec.codecs._

final case class DHRequest(
    nonce: Vector[Byte],
    serverNonce: Vector[Byte],
    pq: Vector[Byte],
    publicKeyFingerprint: Long,
    encrypted: Vector[Byte]
) extends ClientRequest

object DHRequest {

  val ConstructorId: Int = hex"d712e4be".toInt()

  implicit def dhRequestCodec(implicit cf: CipherFactory): Codec[DHRequest] =
    (("nonce" | vectorOfN(provide(16), byte)) ::
      ("server_nonce" | vectorOfN(provide(16), byte)) ::
      ("pq" | paddedFixedSizeBytes(12, vectorOfN(int8, byte), constant(ByteVector.fromByte(0)))) ::
      ("public_key_fingerprint" | int64) ::
      ("encrypted" | encrypted(vector(byte)))).as[DHRequest]

}
