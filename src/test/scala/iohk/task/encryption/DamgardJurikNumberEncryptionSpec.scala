package iohk.task.encryption

import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.Matchers

class DamgardJurikNumberEncryptionSpec extends Properties("DamgardJurikNumberEncryption")
  with Matchers
  with BigIntGeneration
  with DamgardJurikNumberEncryption {

  lazy val keyPair =  new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())

  override lazy val publicKey: DamgardJurikPublicKey = keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey]

  val testEncryptor = new ScDamgardJurikEnc()
  testEncryptor.setKey(keyPair.getPublic, keyPair.getPrivate)

  property("encryptNumber") = forAll(bigIntGen) { bigInt =>
      val expected = testEncryptor.decrypt(testEncryptor.encrypt(new BigIntegerPlainText(bigInt.bigInteger)))
      val actual =  testEncryptor.decrypt(encryptNumber(bigInt))
      expected == actual
  }
}
