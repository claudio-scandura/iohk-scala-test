package iohk.task.actors

import java.security.SecureRandom

import akka.actor.ActorRef
import akka.testkit
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActor, TestActorRef, TestProbe}
import akka.util.Timeout
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductProverComputation, SigmaDJProductProverInput}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText
import iohk.task.ActorSystemWithTestActor
import iohk.task.actors.messages._
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentCaptor, Matchers => MockitoMatchers}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

class BrokerSpec extends WordSpec
  with BeforeAndAfterAll
  with MockitoSugar
  with Matchers {

  class TestSigmaProtocolMessage extends SigmaProtocolMsg

  trait Setup extends ActorSystemWithTestActor {
   
    lazy val autopilot: AutoPilot = new TestActor.AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        sender ! EncryptedNumber(new BigIntegerCiphertext(BigInt(1L).bigInteger))
        TestActor.KeepRunning
      }
    }

    val aliceNumber = new BigIntegerPlainText(BigInt(1L).bigInteger)
    val bobNumber = new BigIntegerPlainText(BigInt(1L).bigInteger)

    val aliceProbe = TestProbe()
    aliceProbe.setAutoPilot(autopilot)

    val bobProbe = TestProbe()
    bobProbe.setAutoPilot(autopilot)

    val usersAskTimeout = Timeout(1.second)

    val resultOfComputation = ResultOfComputation(
      new BigIntegerCiphertext(BigInt(1L).bigInteger),
      new BigIntegerCiphertext(BigInt(1L).bigInteger),
      new BigIntegerCiphertext(BigInt(1L).bigInteger)
    )

    lazy val testProver = new SigmaDJProductProverComputation(40, 1, new SecureRandom())

    lazy val keyPair = new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())

    override lazy val actorUnderTest = TestActorRef(new Broker(aliceProbe.ref, bobProbe.ref, keyPair, usersAskTimeout) {
      override val prover = testProver

      override def receive = logAndHandle(waitingForSigmaDJProtocolStart orElse waitingForProtocolStart)
    })

  }

  "The broker actor" when {

    s"Receiving an $InitProtocol message" should {

      s"ask both Alice and Bob a $GetNumber message" in new Setup {
        actorUnderTest ! InitProtocol

        aliceProbe.expectMsg(GetNumber)
        bobProbe.expectMsg(GetNumber)
      }

      s"respond with $AbortProtocol if Alice or Bob do not provide their numbers in time" in new Setup {

        override lazy val autopilot: AutoPilot = new TestActor.AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = {
            TestActor.KeepRunning
          }
        }

        override val usersAskTimeout = Timeout(100.millis)

        actorUnderTest ! InitProtocol

        expectMsg(AbortProtocol)
      }
    }

    s"Receiving an $InitSigmaDJProductProtocol message" should {

      s"reply with a $SigmaDJProductFirstMessage calculated out of the result of the computation with the ${classOf[SigmaDJProductProverComputation]}" in new Setup {
        override lazy val testProver = mock[SigmaDJProductProverComputation]
        val expectedFirstMessage = new TestSigmaProtocolMessage
        when(testProver.computeFirstMsg(MockitoMatchers.isA(classOf[SigmaDJProductProverInput])))
          .thenReturn(expectedFirstMessage)

        actorUnderTest.underlyingActor.aliceNumber = aliceNumber.getX
        actorUnderTest.underlyingActor.bobNumber = bobNumber.getX
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation

        actorUnderTest ! InitSigmaDJProductProtocol
        val result = expectMsgClass(classOf[SigmaDJProductFirstMessage])
        result.message shouldBe expectedFirstMessage

        val captor = ArgumentCaptor.forClass(classOf[SigmaDJProductProverInput])
        verify(testProver).computeFirstMsg(captor.capture())

        captor.getValue should have(
          'X1 (aliceNumber),
          'X2 (bobNumber)
        )

        captor.getValue.getR1 should not be null
        captor.getValue.getR2 should not be null
        captor.getValue.getR3 should not be null

        captor.getValue.getCommonParams should have(
          'C1 (resultOfComputation.c1),
          'C2 (resultOfComputation.c2),
          'C3 (resultOfComputation.c3),
          'PublicKey (keyPair.getPublic)
        )
      }

      s"accept the challenge and reply with a $SigmaDJProductSecondMessage calculated out of the challenge value" in new Setup {
        override lazy val testProver = mock[SigmaDJProductProverComputation]
        when(testProver.computeFirstMsg(MockitoMatchers.isA(classOf[SigmaDJProductProverInput])))
          .thenReturn(new TestSigmaProtocolMessage)

        actorUnderTest.underlyingActor.aliceNumber = aliceNumber.getX
        actorUnderTest.underlyingActor.bobNumber = bobNumber.getX
        actorUnderTest.underlyingActor.resultOfComputation = resultOfComputation

        val senderProbe = TestProbe()
        actorUnderTest.tell(InitSigmaDJProductProtocol, senderProbe.ref)
        senderProbe.expectMsgClass(classOf[SigmaDJProductFirstMessage])

        val challengeValue = "challenge".getBytes()
        val expectedSecondMessage = new TestSigmaProtocolMessage
        when(testProver.computeSecondMsg(challengeValue)).thenReturn(expectedSecondMessage)
        senderProbe.reply(Challenge(challengeValue))

        val result = senderProbe.expectMsgClass(classOf[SigmaDJProductSecondMessage])
        result.message shouldBe expectedSecondMessage

        verify(testProver).computeSecondMsg(challengeValue)
      }
    }
  }
}
