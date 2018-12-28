package com.mtproto.server.protocol.cipher

import java.security.KeyPairGenerator

import javax.crypto.Cipher
import scodec.bits.ByteVector
import scodec.codecs.CipherFactory

object CipherUtils {

  val PublicRsa: String = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCV/OvX/PqYuvKmpKu9oVDp2B2by3MtD/" +
    "c11Dcq6hSQueEVXD2oE1784oZgzY2PyvIS8jib6WAMtimUxDnXtDOjiBNLpzp9VY55+nrFvkWNB0PeqocAEnwrNUte1Wc/" +
    "YSF1FivbUw4m2KDLBCom4BjVOJIuT5Qp7WVHYD+foWJN9wIDAQAB"

  val sha1PublicRsa: Array[Byte] = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(PublicRsa.getBytes("US-ASCII"))
  }

  val little64bits: ByteVector = {
    val bv = ByteVector(sha1PublicRsa)
    bv.drop(bv.length - 8)
  }

  def cipherFactory(): CipherFactory = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(256 * 8)

    val pair = kpg.generateKeyPair()

    new CipherFactory {
      override def newEncryptCipher: Cipher = {
        val cf = Cipher.getInstance("RSA/ECB/NoPadding")
        cf.init(Cipher.ENCRYPT_MODE, pair.getPublic)
        cf
      }

      override def newDecryptCipher: Cipher = {
        val cf = Cipher.getInstance("RSA/ECB/NoPadding")
        cf.init(Cipher.DECRYPT_MODE, pair.getPrivate)
        cf
      }
    }
  }

}
