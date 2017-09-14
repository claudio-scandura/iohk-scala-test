package iohk.task.actors

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductCommonInput, SigmaDJProductVerifierComputation}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import iohk.task._
import iohk.task.encryption.DamgardJurikNumberEncryption
import messages._
import scala.util.Random

class User(override val publicKey: DamgardJurikPublicKey, verificationAttempts: Int) extends Actor
  with ActorLogging
  with MessageLogging
  with DamgardJurikNumberEncryption {

  val verifier = new SigmaDJProductVerifierComputation(40, 1, new SecureRandom())

  var resultOfComputation: ResultOfComputation = _

  var firstMessage: SigmaProtocolMsg = _

  var numberOfVerificationAttempts = verificationAttempts

  override def receive = logAndHandle {
    case GetNumber =>
      val randomBigInt = BigInt(100, Random)
      val encryptedNumber = encryptNumber(randomBigInt)
      log.debug(s"Responding with $randomBigInt which is encrypted as $encryptedNumber")
      sender ! EncryptedNumber(encryptedNumber)
    case roc@ResultOfComputation(c1, c2, c3) =>
      log.debug(s"Received the following encrypted number a: $c1 b: $c2 and r: $c3")
      resultOfComputation = roc
      sender() ! InitSigmaDJProductProtocol

    case SigmaDJProductFirstMessage(message) =>
      firstMessage = message
      verifier.sampleChallenge()
      sender() ! Challenge(verifier.getChallenge)

    case SigmaDJProductSecondMessage(message) =>
      val commonInput = new SigmaDJProductCommonInput(publicKey, resultOfComputation.c1, resultOfComputation.c2, resultOfComputation.c3)
      val isVerified = verifier.verify(commonInput, firstMessage, message)
      log.debug(s"Verification is: $isVerified")
      numberOfVerificationAttempts -= 1
      sender() ! (if (numberOfVerificationAttempts > 0) InitSigmaDJProductProtocol else Done)

  }
}
