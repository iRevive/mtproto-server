package com.mtproto.server.auth

import scala.collection.concurrent.TrieMap

trait ClientStorage {

  def find(nonce: Vector[Byte]): Option[Client]

  def put(client: Client): Unit

}

object ClientStorage {

  def inMemoryClientStorage(): ClientStorage = new ClientStorage {
    val storage: TrieMap[Vector[Byte], Client] = TrieMap.empty[Vector[Byte], Client]

    override def find(nonce: Vector[Byte]): Option[Client] = storage.get(nonce)

    override def put(client: Client): Unit = {
      val _ = storage.putIfAbsent(client.nonce, client)
    }
  }

}
