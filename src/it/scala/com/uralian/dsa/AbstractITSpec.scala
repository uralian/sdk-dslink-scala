package com.uralian.dsa

import com.typesafe.config.ConfigFactory
import org.scalatest._

/**
  * Base trait for integration test specifications.
  */
trait AbstractITSpec extends Suite
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with OptionValues {

  val config = ConfigFactory.load("integration.conf")

  val brokerUrl = config.getString("dsa.test.brokerUrl")
}