package iohk.task.actors

import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext


package object messages {

  case object InitProtocol

  case object AbortProtocol

  case object GetNumber

  case class EncryptedNumber(encrypted: BigIntegerCiphertext)

  case class ResultOfComputation(c1: BigIntegerCiphertext, c2: BigIntegerCiphertext, c3: BigIntegerCiphertext)

  case object InitSigmaDJProductProtocol

  case class SigmaDJProductFirstMessage(message: SigmaProtocolMsg)

  case class Challenge(challenge: Array[Byte])

  case class SigmaDJProductSecondMessage(message: SigmaProtocolMsg)

  case object Done
}
