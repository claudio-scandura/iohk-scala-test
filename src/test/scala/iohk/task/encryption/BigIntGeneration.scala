package iohk.task.encryption

import org.scalacheck.Gen

trait BigIntGeneration {

  def bigIntGen: Gen[BigInt] = for {
    bi <- Gen.chooseNum(0, Long.MaxValue).map(BigInt.apply)
    n <- Gen.chooseNum(0, 1000)
  } yield bi << n
}
