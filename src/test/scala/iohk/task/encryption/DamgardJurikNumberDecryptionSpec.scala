package iohk.task.encryption

import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.{DamgardJurikPrivateKey, DamgardJurikPublicKey}
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.Matchers

class DamgardJurikNumberDecryptionSpec extends Properties("DamgardJurikNumberDecryption")
  with Matchers
  with BigIntGeneration
  with DamgardJurikNumberDecryption {

  lazy val keyPair = new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())

  override lazy val publicKey: DamgardJurikPublicKey = keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey]
  override lazy val privateKey: DamgardJurikPrivateKey = keyPair.getPrivate.asInstanceOf[DamgardJurikPrivateKey]

  val testEncryptor = new ScDamgardJurikEnc()
  testEncryptor.setKey(keyPair.getPublic, keyPair.getPrivate)

  property("decryptNumber") = forAll(bigIntGen) { bigInt =>
    val encrypted = testEncryptor.encrypt(new BigIntegerPlainText(bigInt.bigInteger))


    val expected = testEncryptor.decrypt(encrypted)
    val actual = decryptNumber(encrypted.asInstanceOf[BigIntegerCiphertext])
    expected == actual
  }
}
