package iohk.task

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

trait Configuration {
  def config: Config

  lazy val verificationAttempts: Int = config.getInt("verification-attempts")
  lazy val endProtocolMaxWait: FiniteDuration = config.getDuration("end-protocol-max-wait-seconds")
  lazy val getNumbersMaxWait: FiniteDuration = config.getDuration("get-numbers-max-wait-seconds")

  implicit def javaToScalaDuration(javaDuration: java.time.Duration): FiniteDuration = FiniteDuration(javaDuration.toNanos, TimeUnit.NANOSECONDS)
}
