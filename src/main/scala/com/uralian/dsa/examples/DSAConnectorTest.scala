package com.uralian.dsa.examples

import java.util.concurrent.CountDownLatch

import com.uralian.dsa.{DSAEventListener, LinkMode, RichNodeBuilder, intToValue, valueToInt}
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.methods.requests.{InvokeRequest, ListRequest}
import org.dsa.iot.dslink.methods.responses.ListResponse
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.util.handler.Handler

import scala.collection.JavaConverters.mapAsScalaMapConverter

/**
  * Demonstrates connecting to the broker and doing basic operations.
  */
object DSAConnectorTest extends App {

  {
    println("---------------------------------------------\nTesting Responder mode")
    val connector = createConnector(args)
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)
    })
    println("Starting connector")
    val connection = connector.start(LinkMode.RESPONDER)
    assert(connection.isResponder)
    assert(!connection.isRequester)

    println("""Creating a child under name "Test Child" with value of 5""")
    val root = connection.responderLink.getNodeManager.getSuperRoot
    root createChild("test-child", "node") display "Test Child" valueType ValueType.DYNAMIC value 5 build

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  {
    println("---------------------------------------------\nTesting Requester mode")
    val connector = createConnector(args)
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onRequesterConnected(link: DSLink) = println("requester link connected: " + link.isConnected)
    })
    println("Starting connector")
    val connection = connector.start(LinkMode.REQUESTER)
    assert(!connection.isResponder)
    assert(connection.isRequester)

    println("Listing children of /sys node")
    val latch = new CountDownLatch(1)
    connection.requester.list(new ListRequest("/sys"), new Handler[ListResponse] {
      def handle(event: ListResponse) = {
        event.getUpdates.asScala.toMap foreach {
          case (node, flag) =>
            val vType = Option(node.getValueType) map (_.toJsonString) getOrElse "n/a"
            println(s"""${node.getPath}: $vType ${if (flag) " DELETED" else ""}""")
        }
        latch.countDown
      }
    })
    latch.await

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  {
    println("---------------------------------------------\nTesting Dual mode")
    val connector = createConnector(args)
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)

      override def onRequesterConnected(link: DSLink) = println("requester link connected: " + link.isConnected)
    })
    println("Starting connector")
    val connection = connector.start(LinkMode.DUAL)
    assert(connection.isResponder)
    assert(connection.isRequester)

    // responder
    val root = connection.responderLink.getNodeManager.getSuperRoot
    root createChild("counter", "node") valueType ValueType.NUMBER value 0 build()
    root createChild("incCounter", "node") display "Increment counter" action (event => {
      val node = event.getNode.getParent.getChild("counter", true)
      val oldVal = node.getValue: Int
      node.setValue(oldVal + 1)
    }) build()

    // requester
    val req = new InvokeRequest(connection.requesterLink.getPath + "/incCounter")
    println(req.getPath)
    connection.requester.invoke(req, event => println("successfully invoked: " + event.getState))

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  System.exit(0)
}