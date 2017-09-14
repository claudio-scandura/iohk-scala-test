package iohk.task

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}

abstract class ActorSystemWithTestActor extends TestKit(ActorSystem("TestActorSystem"))
  with ImplicitSender {

  def actorUnderTest: TestActorRef[_]

  def apply(fun: => Unit) = try {
    actorUnderTest.start()
    fun
  } finally {
    actorUnderTest.stop()
    TestKit.shutdownActorSystem(system)
  }
}
