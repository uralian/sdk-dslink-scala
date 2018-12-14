package com.uralian.dsa.util

import com.uralian.dsa.AbstractUnitSpec

/**
  * TestUtils test suite.
  */
class TestUtilsSpec extends AbstractUnitSpec {

  "TestUtils" should {
    "create a connector" in {
      val connector = TestUtils.createConnector("http://localhost:8080")
      connector should not be 'connected
    }
  }
}
