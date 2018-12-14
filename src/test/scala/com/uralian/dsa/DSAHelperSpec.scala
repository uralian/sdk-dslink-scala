package com.uralian.dsa

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.link.{Linkable, Requester, Responder}
import org.dsa.iot.dslink.methods.requests.{InvokeRequest, ListRequest, RemoveRequest, SetRequest}
import org.dsa.iot.dslink.methods.responses._
import org.dsa.iot.dslink.node.value.{SubscriptionValue, Value, ValueType}
import org.dsa.iot.dslink.node.{Node, NodeManager, NodePair, SubscriptionManager}
import org.dsa.iot.dslink.util.handler.Handler
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

import scala.collection.JavaConverters._

/**
  * DSAHelper test suite.
  */
class DSAHelperSpec extends AbstractUnitSpec with MockitoSugar with ScalaFutures {

  val ec = scala.concurrent.ExecutionContext.Implicits.global

  "DSAHelper" should {
    "invoke actions" in {
      val requester = mock[Requester]
      val request = argumentCaptor[InvokeRequest]
      DSAHelper.invoke("/action", "a" -> 1)(requester)
      verify(requester).invoke(request.capture(), handler[InvokeResponse])
      request.getValue.getPath shouldBe "/action"
    }
    "invoke actions and wait for response" in {
      val requester = mock[Requester]
      val request = argumentCaptor[InvokeRequest]
      DSAHelper.invokeAndWait("/action", "a" -> 1)(requester)
      verify(requester).invoke(request.capture(), handler[InvokeResponse])
      request.getValue.getPath shouldBe "/action"
    }
    "list node contents" in {
      val requester = mock[Requester]
      val request = argumentCaptor[ListRequest]
      DSAHelper.list("/downstream")(requester).subscribe(_ => {})
      verify(requester).list(request.capture(), handler[ListResponse])
      request.getValue.getPath shouldBe "/downstream"
    }
    "watch for node updates" in {
      val requester = mock[Requester]
      val request = argumentCaptor[String]
      DSAHelper.watch("/nodeA")(requester).subscribe(_ => {}).unsubscribe()
      verify(requester).subscribe(request.capture(), handler[SubscriptionValue])
      DSAHelper.unwatch("/nodeA")(requester)
      verify(requester).unsubscribe(request.capture(), handler[UnsubscribeResponse])
      request.getAllValues.asScala shouldBe List("/nodeA", "/nodeA")
    }
    "set node value" in {
      val requester = mock[Requester]
      val request = argumentCaptor[SetRequest]
      DSAHelper.set("/nodeB", 111)(requester)
      verify(requester).set(request.capture(), handler[SetResponse])
      request.getValue.getPath shouldBe "/nodeB"
    }
    "remove node" in {
      val requester = mock[Requester]
      val request = argumentCaptor[RemoveRequest]
      DSAHelper.remove("/nodeC")(requester)
      verify(requester).remove(request.capture(), handler[RemoveResponse])
      request.getValue.getPath shouldBe "/nodeC"
    }
    "close subscription" in {
      val requester = mock[Requester]
      val request = argumentCaptor[Int]
      DSAHelper.close(111)(requester)
      verify(requester).closeStream(request.capture(), handler[CloseResponse])
      request.getValue shouldBe 111
    }
    "retrieve children nodes" in {
      val requester = mock[Requester]
      val request = argumentCaptor[ListRequest]
      DSAHelper.getNodeChildren("/downstream")(requester).subscribe(_ => {})
      verify(requester).list(request.capture(), handler[ListResponse])
      request.getValue.getPath shouldBe "/downstream"
    }
    "retrieve node value as Observable" in {
      val requester = mock[Requester]
      val request = argumentCaptor[String]
      DSAHelper.getNodeValue("/nodeD")(requester, ec)
      verify(requester).subscribe(request.capture(), handler[SubscriptionValue])
      request.getValue shouldBe "/nodeD"
    }
    "update node" in {
      val link = mock[DSLink]
      val responder = mock[Responder]
      val manager = mock[NodeManager]
      val linkable = mock[Linkable]
      val subManager = mock[SubscriptionManager]
      val node = new Node("abc", null, linkable)
      val pair = new NodePair(node, "")
      when(responder.getDSLink).thenReturn(link)
      when(link.getNodeManager).thenReturn(manager)
      when(linkable.getSubscriptionManager).thenReturn(subManager)
      when(manager.getNode(anyString, anyBoolean)).thenReturn(pair)
      DSAHelper.updateNode("/nodeE" -> 123)(responder)
      node.getValueType shouldBe ValueType.NUMBER
      node.getValue shouldBe (123: Value)
    }
  }

  private def handler[T] = any[Handler[T]]()
}
