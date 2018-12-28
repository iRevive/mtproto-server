package com.mtproto.server.auth

final case class Client(nonce: Vector[Byte], serverNonce: Vector[Byte], pq: Vector[Byte])
