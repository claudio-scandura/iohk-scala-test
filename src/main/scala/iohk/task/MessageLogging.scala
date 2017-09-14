package iohk.task

import akka.actor.Actor.Receive
import akka.actor.ActorLogging

trait MessageLogging {
  self: ActorLogging =>

  def logAndHandle(handler: Receive): Receive = {
    case msg: Any =>
      log.info(s"Received message $msg")
      handler(msg)
  }
}
