package com.uralian.dsa

import org.dsa.iot.dslink.link._
import org.dsa.iot.dslink.{DSLink, DSLinkProvider}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

/**
  * DSAConnector test suite.
  */
class DSAConnectorSpec extends AbstractUnitSpec with MockitoSugar {

  val connector = DSAConnector.create("http://localhost:8080/conn")
  val listener = mock[DSAEventListener]

  val provider = mock[DSLinkProvider]
  val requesterLink = mock[DSLink]
  val requester = mock[Requester]
  when(requesterLink.getRequester).thenReturn(requester)
  val responderLink = mock[DSLink]
  val responder = mock[Responder]
  when(responderLink.getResponder).thenReturn(responder)

  "DSAConnector" should {
    "return status" in {
      connector should not be 'connected
    }
    "support listeners" in {
      noException should be thrownBy connector.addListener(listener)
      noException should be thrownBy connector.removeListener(listener)
      noException should be thrownBy connector.clearListeners()
    }
  }

  "DSAConnection" should {
    "handle RESPONDER mode" in {
      val conn = DSAConnection(LinkMode.RESPONDER, provider, responderLink, null)
      conn shouldBe 'responder
      conn should not be 'requester
      conn.responder shouldBe responder
      conn.requester shouldBe null
    }
    "handle REQUESTER mode" in {
      val conn = DSAConnection(LinkMode.REQUESTER, provider, null, requesterLink)
      conn shouldBe 'requester
      conn should not be 'responder
      conn.responder shouldBe null
      conn.requester shouldBe requester
    }
    "handle DUAL mode" in {
      val conn = DSAConnection(LinkMode.DUAL, provider, responderLink, requesterLink)
      conn shouldBe 'requester
      conn shouldBe 'responder
      conn.responder shouldBe responder
      conn.requester shouldBe requester
    }
  }
}
