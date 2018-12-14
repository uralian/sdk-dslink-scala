package com.uralian.dsa

import com.uralian.dsa.util.TestUtils
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

/**
  * DSAConnector test suite.
  */
class DSAConnectorITSpec extends AbstractITSpec with MockitoSugar {

  val connector = TestUtils.createConnector(brokerUrl)

  "DSAConnector" should {
    "connect as RESPONDER and publish lifecycle events" in {
      val listener = mock[DSAEventListener]
      connector.addListener(listener)

      val connection = connector.start(LinkMode.RESPONDER)
      connection shouldBe 'responder
      connection should not be 'requester

      Thread.sleep(500)
      verify(listener).onResponderInitialized(connection.responderLink)
      verify(listener).onResponderConnected(connection.responderLink)

      connector.stop()
      connector.removeListener(listener)
    }
    "connect as REQUESTER and publish lifecycle events" in {
      val listener = mock[DSAEventListener]
      connector.addListener(listener)

      val connection = connector.start(LinkMode.REQUESTER)
      connection shouldBe 'requester
      connection should not be 'responder

      Thread.sleep(500)
      verify(listener).onRequesterInitialized(connection.requesterLink)
      verify(listener).onRequesterConnected(connection.requesterLink)

      connector.stop()
      connector.removeListener(listener)
    }
    "connect as DUAL and publish lifecycle events" in {
      val listener = mock[DSAEventListener]
      connector.addListener(listener)

      val connection = connector.start
      connection shouldBe 'requester
      connection shouldBe 'responder

      Thread.sleep(500)
      verify(listener).onRequesterInitialized(connection.requesterLink)
      verify(listener).onRequesterConnected(connection.requesterLink)
      verify(listener).onResponderInitialized(connection.responderLink)
      verify(listener).onResponderConnected(connection.responderLink)

      connector.stop()
      connector.removeListener(listener)
    }
    "fail when ordered to connect when already connected" in {
      connector.start
      a[IllegalStateException] should be thrownBy connector.start
      connector.stop()
    }
  }

  override protected def afterAll(): Unit = {
    connector.clearListeners()
    if (connector.isConnected) connector.stop()
  }
}
