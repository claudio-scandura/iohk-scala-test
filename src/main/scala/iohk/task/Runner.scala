package iohk.task

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DJKeyGenParameterSpec, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import iohk.task.actors.{Broker, User}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Runner extends App with Configuration {

  override val config = ConfigFactory.load()

  implicit val actorSystem = ActorSystem("iohk-task-system")
  implicit val timeout = Timeout(endProtocolMaxWait)

  val keyPair =  new ScDamgardJurikEnc().generateKey(new DJKeyGenParameterSpec())
  val alice = actorSystem.actorOf(Props(new User(keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey], verificationAttempts)), "Alice")
  val bob = actorSystem.actorOf(Props(new User(keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey], verificationAttempts)), "Bob")

  val broker = actorSystem.actorOf(Props(new Broker(alice, bob, keyPair, getNumbersMaxWait)), "Carroll")

  (broker ? actors.messages.InitProtocol).onComplete {
    case result =>
      println(s"Result of protocol is: $result")
      Await.ready(actorSystem.terminate(), 10.seconds)
      System.exit(0)
  }


}
