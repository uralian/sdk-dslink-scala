package com.uralian.dsa

import com.uralian.dsa.util.TestUtils

/**
  * Helper methods used by examples.
  */
package object examples {

  val DEFAULT_BROKER_URL = "http://localhost:8080/conn"

  def createConnector(args: Array[String]) = {
    val brokerUrl = if (args.length < 1)
      DEFAULT_BROKER_URL having println(s"Broker URL not specified, using the default one: $DEFAULT_BROKER_URL")
    else
      args(0) having (x => println(s"Broker URL: $x"))

    TestUtils.createConnector(brokerUrl)
  }
}