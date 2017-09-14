package iohk.task.actors

import java.security.{KeyPair, SecureRandom}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern._
import akka.util.Timeout
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductProverComputation, SigmaDJProductProverInput}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.{DamgardJurikPrivateKey, DamgardJurikPublicKey}
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText
import iohk.task._
import iohk.task.encryption.{DamgardJurikNumberDecryption, DamgardJurikNumberEncryption}
import messages._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class Broker(alice: ActorRef, bob: ActorRef, keyPair: KeyPair, usersAskTimeout: Timeout) extends Actor
  with ActorLogging
  with MessageLogging
  with DamgardJurikNumberDecryption
  with DamgardJurikNumberEncryption {

  override lazy val publicKey = keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey]
  override lazy val privateKey = keyPair.getPrivate.asInstanceOf[DamgardJurikPrivateKey]

  implicit val timeout = usersAskTimeout

  val prover = new SigmaDJProductProverComputation(40, 1, new SecureRandom())
  var initProtocolRequestor: ActorRef = _
  var aliceNumber: BigInt = _
  var bobNumber: BigInt = _
  var resultOfComputation: ResultOfComputation = _

  var aliceIsDone = false
  var bobIsDone = false

  override def receive = logAndHandle(waitingForProtocolStart.orElse(logUnexpectedMessage("waitingForProtocolStart")))

  private[actors] def waitingForProtocolStart: Receive =  {
    case InitProtocol =>
      initProtocolRequestor = sender()
      val aliceReply = alice ? GetNumber
      val bobReply = bob ? GetNumber

      Future.sequence(Seq(aliceReply, bobReply)).onComplete {
        case Success(Seq(EncryptedNumber(a), EncryptedNumber(b))) =>
          aliceNumber = decryptNumber(a).getX
          bobNumber = decryptNumber(b).getX
          val multiplication = aliceNumber * bobNumber
          log.debug(s"Alice replied: $aliceNumber and Bob replied: $bobNumber. Result of multiplication: $multiplication")

          resultOfComputation = ResultOfComputation(encryptNumber(aliceNumber), encryptNumber(bobNumber), encryptNumber(multiplication))

          alice ! resultOfComputation
          bob ! resultOfComputation
          context.become(waitingForSigmaDJProtocolStart.orElse(logUnexpectedMessage("waitingForSigmaDJProtocolStart")))
        case Failure(e) =>
          log.warning(s"Failed to get responses from Alice and Bob because of $e. Aborting.")
          initProtocolRequestor ! AbortProtocol
      }
  }

  private[actors] def waitingForSigmaDJProtocolStart: Receive =  {
    case InitSigmaDJProductProtocol =>
      val challengeF = sender() ? buildFirstMessage(
        resultOfComputation.c1,
        resultOfComputation.c2,
        resultOfComputation.c3,
        new BigIntegerPlainText(aliceNumber.bigInteger),
        new BigIntegerPlainText(bobNumber.bigInteger)
      )
      challengeF.map {
        case Challenge(c) => SigmaDJProductSecondMessage(prover.computeSecondMsg(c))
      }.pipeTo(sender())

    case Done =>
      if (sender() == alice)
        aliceIsDone = true
      else if (sender() == bob)
        bobIsDone = true
      if (aliceIsDone && bobIsDone) {
        initProtocolRequestor ! Done
        aliceIsDone = false
        bobIsDone = false
        context.become(waitingForProtocolStart.orElse(logUnexpectedMessage("waitingForProtocolStart")))
      }

  }

  private def buildFirstMessage(c1: BigIntegerCiphertext, c2: BigIntegerCiphertext, c3: BigIntegerCiphertext, x1: BigIntegerPlainText, x2: BigIntegerPlainText) = {
    val proverInput = new SigmaDJProductProverInput(
      publicKey,
      c1, c2, c3,
      privateKey,
      x1, x2)
    SigmaDJProductFirstMessage(prover.computeFirstMsg(proverInput))
  }

  private def logUnexpectedMessage(state: String): Receive = {
    case other => log.info(s"Unexpected message $other while $state")
  }
}
