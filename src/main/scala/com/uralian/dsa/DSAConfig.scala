package com.uralian.dsa

import com.typesafe.config.ConfigFactory

/**
 * DSA Configuration backed by Typesafe config.
 */
object DSAConfig {
  private lazy val config = ConfigFactory.load.getConfig("dsa")

  lazy val connectTimeout = config.getLong("connect.timeout")
}