package iohk.task.encryption

import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ScDamgardJurikEnc
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText

trait DamgardJurikNumberEncryption {

  def publicKey: DamgardJurikPublicKey

  private val encryptor = {
    val e = new ScDamgardJurikEnc()
    e.setKey(publicKey)
    e
  }

  def encryptNumber(message: BigInt): BigIntegerCiphertext = encryptor
      .encrypt(new BigIntegerPlainText(message.bigInteger))
      .asInstanceOf[BigIntegerCiphertext]
}
