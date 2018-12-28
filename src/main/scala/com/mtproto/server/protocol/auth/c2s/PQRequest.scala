package com.mtproto.server.protocol.auth
package c2s

import scodec._
import scodec.bits._
import scodec.codecs._

final case class PQRequest(nonce: Vector[Byte]) extends ClientRequest

object PQRequest {

  val ConstructorId: Int = hex"60469778".toInt()

  implicit val pqRequestCodec: Codec[PQRequest] =
    ("nonce" | vectorOfN(provide(16), byte)).as[PQRequest]

}
