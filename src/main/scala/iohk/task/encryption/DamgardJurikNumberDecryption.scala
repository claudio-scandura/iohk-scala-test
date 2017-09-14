package iohk.task.encryption

import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ScDamgardJurikEnc
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.{DamgardJurikPrivateKey, DamgardJurikPublicKey}
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText

trait DamgardJurikNumberDecryption {

  def publicKey: DamgardJurikPublicKey

  def privateKey: DamgardJurikPrivateKey

  private val encryptor = {
    val e = new ScDamgardJurikEnc()
    e.setKey(publicKey, privateKey)
    e
  }

  def decryptNumber(message: BigIntegerCiphertext): BigIntegerPlainText = encryptor
    .decrypt(message)
    .asInstanceOf[BigIntegerPlainText]
}
