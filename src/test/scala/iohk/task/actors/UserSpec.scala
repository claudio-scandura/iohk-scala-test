package iohk.task.actors

import java.security.SecureRandom

import akka.testkit.TestActorRef
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductCommonInput, SigmaDJProductVerifierComputation}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import iohk.task.ActorSystemWithTestActor
import iohk.task.actors.messages._
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentCaptor, Matchers => MockitoMatchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class UserSpec extends WordSpec
  with BeforeAndAfterAll
  with MockitoSugar
  with Eventually
  with Matchers {

  class TestSigmaProtocolMessage extends SigmaProtocolMsg

  trait Setup extends ActorSystemWithTestActor {
    lazy val keyPair = new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())

    val resultOfComputation = ResultOfComputation(
      new BigIntegerCiphertext(BigInt(1L).bigInteger),
      new BigIntegerCiphertext(BigInt(1L).bigInteger),
      new BigIntegerCiphertext(BigInt(1L).bigInteger)
    )

    lazy val testVerifier = new SigmaDJProductVerifierComputation(40, 1, new SecureRandom())
    lazy val verificationAttempts = 1

    override lazy val actorUnderTest = TestActorRef(new User(keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey], verificationAttempts) {
      override val verifier = testVerifier
    })

  }

  "The user actor" when {

    s"receiving the message $GetNumber" should {

      s"respond with a $EncryptedNumber message" in new Setup {
        actorUnderTest ! GetNumber
        val response = expectMsgClass(classOf[EncryptedNumber])
        response.encrypted shouldNot be(null)
      }
    }

    s"receiving the message $ResultOfComputation" should {

      "save the message" in new Setup {
        actorUnderTest ! resultOfComputation
        eventually(actorUnderTest.underlyingActor should have('resultOfComputation (resultOfComputation)))
      }

      s"start the ZK proof protocol by sending a $InitSigmaDJProductProtocol" in new Setup {
        actorUnderTest ! resultOfComputation
        expectMsg(InitSigmaDJProductProtocol)
      }
    }

    s"receiving the message $SigmaDJProductFirstMessage" should {

      "save the message firstMessage" in new Setup {
        val content = new TestSigmaProtocolMessage
        actorUnderTest ! SigmaDJProductFirstMessage(content)
        eventually(actorUnderTest.underlyingActor should have('firstMessage (content)))
      }

      s"respond with a sampled challenge in a $Challenge message" in new Setup {
        val firstMessage = new TestSigmaProtocolMessage
        actorUnderTest ! SigmaDJProductFirstMessage(firstMessage)

        val result = expectMsgClass(classOf[Challenge])
        result.challenge should not be empty
      }
    }

    s"receiving the message $SigmaDJProductSecondMessage" should {

      s"verify the statement by calling the ${classOf[SigmaDJProductVerifierComputation]}" in new Setup {
        val firstMessage = new TestSigmaProtocolMessage
        override lazy val testVerifier = mock[SigmaDJProductVerifierComputation]
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation
        actorUnderTest.underlyingActor.firstMessage = firstMessage

        val secondMessage = new TestSigmaProtocolMessage
        actorUnderTest ! SigmaDJProductSecondMessage(secondMessage)

        eventually {
          val captor = ArgumentCaptor.forClass(classOf[SigmaDJProductCommonInput])
          verify(testVerifier).verify(captor.capture(), MockitoMatchers.eq(firstMessage), MockitoMatchers.eq(secondMessage))
          captor.getValue should have(
            'c1 (resultOfComputation.c1),
            'c2 (resultOfComputation.c2),
            'c3 (resultOfComputation.c3)
          )
        }
      }

      "decrement the number of verification attempts left to do if verification succeeds" in new Setup {
        override lazy val testVerifier = mock[SigmaDJProductVerifierComputation]
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation

        when(testVerifier.verify(MockitoMatchers.any(), MockitoMatchers.any(), MockitoMatchers.any())).thenReturn(true)
        actorUnderTest ! SigmaDJProductSecondMessage(new TestSigmaProtocolMessage)

        eventually(actorUnderTest.underlyingActor.numberOfVerificationAttempts shouldBe verificationAttempts - 1)
      }

      "re-init ZK proof is there are verification attempts left" in new Setup {
        override lazy val verificationAttempts = 2
        override lazy val testVerifier = mock[SigmaDJProductVerifierComputation]
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation

        when(testVerifier.verify(MockitoMatchers.any(), MockitoMatchers.any(), MockitoMatchers.any())).thenReturn(true)
        actorUnderTest ! SigmaDJProductSecondMessage(new TestSigmaProtocolMessage)

        expectMsg(InitSigmaDJProductProtocol)
      }

      s"send $Done message if there are no attempts left" in new Setup {
        override lazy val verificationAttempts = 1
        override lazy val testVerifier = mock[SigmaDJProductVerifierComputation]
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation

        when(testVerifier.verify(MockitoMatchers.any(), MockitoMatchers.any(), MockitoMatchers.any())).thenReturn(true)
        actorUnderTest ! SigmaDJProductSecondMessage(new TestSigmaProtocolMessage)

        expectMsg(Done)
      }
    }
  }
}
