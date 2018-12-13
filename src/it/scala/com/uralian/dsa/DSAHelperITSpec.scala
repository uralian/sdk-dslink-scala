package com.uralian.dsa

import java.util.UUID

import com.uralian.dsa.util.TestUtils
import org.dsa.iot.dslink.methods.StreamState
import org.scalatest.Inspectors
import org.scalatest.concurrent.ScalaFutures

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * DSAHelper test suite.
  */
class DSAHelperITSpec extends AbstractITSpec with ScalaFutures with Inspectors {

  val connector = TestUtils.createConnector(brokerUrl)
  val connection = connector.start

  implicit val requester = connection.requester

  "DSAHelper" should {
    "invoke actions" in {
      val flowName1 = UUID.randomUUID().toString
      val obsCreate = DSAHelper.invoke("/downstream/dataflow/createDataflow", "name" -> flowName1)
      whenReady(obsCreate.toBlocking.toFuture) {
        _.getState shouldBe StreamState.CLOSED
      }
    }
    "invoke actions and wait for response" in {
      val flowName1 = UUID.randomUUID().toString
      val fCreate = DSAHelper.invokeAndWait("/downstream/dataflow/createDataflow", "name" -> flowName1)
      whenReady(fCreate) {
        _.getState shouldBe StreamState.CLOSED
      }
    }
    "list node contents" in {
      val obsSystem = DSAHelper list "/downstream/System"
      val response = obsSystem.head.toBlocking.single
      val children = response.getUpdates.asScala.collect {
        case (node, java.lang.Boolean.FALSE) => node.getName
      }
      children should contain allOf("CPU_Usage", "Memory_Usage", "Architecture")
      DSAHelper close response.getRid
    }
    "watch for node updates" in {
      val obsCpu = DSAHelper watch "/downstream/System/CPU_Usage"
      val updates = obsCpu.take(1 second).toBlocking.toList
      forAll(updates) {
        _.getPath shouldBe "/downstream/System/CPU_Usage"
      }
      DSAHelper unwatch "/downstream/System/CPU_Usage"
    }
    "retrieve node children as Observable" in {
      val allChildren = DSAHelper getNodeChildren "/downstream/System"
      val ten = allChildren.take(10).toBlocking.toList
      ten.map(_.getName) should contain atLeastOneOf("CPU_Usage", "Memory_Usage", "Architecture", "Open_Files",
        "Disk_Usage", "Used_Memory", "Platform", "Model")
    }
    "retrieve node value as Observable" in {
      whenReady(DSAHelper getNodeValue "/downstream/System/CPU_Usage") {
        case (path, _, value: Number) =>
          path shouldBe "/downstream/System/CPU_Usage"
          value.intValue should (be >= 0 and be <= 100)
        case _                        => fail("invalid response for getNodeValue")
      }
    }
  }

  override protected def afterAll(): Unit = {
    connector.clearListeners()
    if (connector.isConnected) connector.stop()
  }
}
