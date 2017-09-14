package iohk.task

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import iohk.task.actors.messages.{Done, InitProtocol}
import iohk.task.actors.{Broker, User}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class BrokerWithUsersISpec extends WordSpec with ScalaFutures with Matchers with BeforeAndAfterAll {

  implicit val actorSystem = ActorSystem("iohk-task-system")
  implicit val timeout = Timeout(10.seconds)

  s"Sending a $InitProtocol message to Carrol the broker" should {

    "trigger the numbers exchange protocol and finish after the users Alice and Bob have verified Carrol's statement with the ZK proof" in {
      val numberOfVerificationsAttempts = 2
      val keyPair = new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())
      val alice = actorSystem.actorOf(Props(new User(keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey], numberOfVerificationsAttempts)), "Alice")
      val bob = actorSystem.actorOf(Props(new User(keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey], numberOfVerificationsAttempts)), "Bob")

      val broker = actorSystem.actorOf(Props(new Broker(alice, bob, keyPair, Timeout(1.seconds))), "Carroll")

      Await.result(broker ? actors.messages.InitProtocol, 10.seconds) shouldBe Done
    }
  }

  override protected def afterAll(): Unit = Await.ready(actorSystem.terminate(), 10.seconds)
}
