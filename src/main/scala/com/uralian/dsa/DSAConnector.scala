package com.uralian.dsa

import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.dsa.iot.dslink.{DSLink, DSLinkFactory, DSLinkHandler, DSLinkProvider}
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.util.control.NonFatal

/**
  * DSA Link mode.
  */
object LinkMode extends Enumeration {

  abstract class LinkMode extends super.Val {
    def isResponder: Boolean

    def isRequester: Boolean
  }

  implicit def valueToLinkMode(v: Value) = v.asInstanceOf[LinkMode]

  val RESPONDER = new LinkMode {
    val isResponder = true
    val isRequester = false
  }

  val REQUESTER = new LinkMode {
    val isResponder = false
    val isRequester = true
  }

  val DUAL = new LinkMode {
    val isResponder = true
    val isRequester = true
  }
}

import com.uralian.dsa.LinkMode._

/**
  * DSA Connection data.
  */
case class DSAConnection(linkMode: LinkMode, provider: DSLinkProvider,
                         responderLink: DSLink, requesterLink: DSLink) {

  val isResponder = linkMode.isResponder
  val isRequester = linkMode.isRequester

  assert(isResponder == (responderLink != null))
  assert(isRequester == (requesterLink != null))

  val responder = if (isResponder) responderLink.getResponder else null
  val requester = if (isRequester) requesterLink.getRequester else null
}

/**
  * Exhibits method for initiating and stopping a connection to a DSA broker.
  */
class DSAConnector private(args: Iterable[String]) {

  private val log = LoggerFactory.getLogger(getClass)

  private var provider: DSLinkProvider = null

  private var listeners = Set.empty[DSAEventListener]

  /**
    * Registers a new listener.
    */
  def addListener(listener: DSAEventListener) = synchronized(listeners += listener)

  /**
    * Unregisters a listener.
    */
  def removeListener(listener: DSAEventListener) = synchronized(listeners -= listener)

  /**
    * Removes all listeners.
    */
  def clearListeners() = synchronized(listeners = Set.empty[DSAEventListener])

  //$COVERAGE-OFF$
  /**
    * Connects to the DSA broker in DUAL mode and returns the DSAConnection instance.
    *
    * @throws IllegalStateException if the connection has already been started.
    */
  def start: DSAConnection = start(DUAL)

  /**
    * Connects to the DSA broker and returns the DSAConnection instance.
    *
    * @param linkMode the link mode (REQUESTER, RESPONDER, or DUAL).
    * @return an instance of DSAConnection.
    * @throws IllegalStateException if the connection has already been started.
    */
  def start(linkMode: LinkMode = DUAL): DSAConnection = synchronized {

    if (isConnected)
      throw new IllegalStateException("Already connected")
    else {
      val latch = new CountDownLatch(if (linkMode == DUAL) 2 else 1)

      var rspLink: DSLink = null
      var reqLink: DSLink = null

      provider = DSLinkFactory.generate(args.toArray, new DSLinkHandler {
        override val isResponder = linkMode.isResponder
        override val isRequester = linkMode.isRequester

        override def onResponderInitialized(link: DSLink) = {
          log.info("Responder initialized")
          listeners foreach (_.onResponderInitialized(link))
        }

        override def onResponderConnected(link: DSLink) = {
          log.info("Responder connected")
          rspLink = link
          latch.countDown
          listeners foreach (_.onResponderConnected(link))
        }

        override def onResponderDisconnected(link: DSLink) = {
          log.warn("Responder disconnected from the broker")
          listeners foreach (_.onResponderDisconnected(link))
        }

        override def onRequesterInitialized(link: DSLink) = {
          log.info("Requester initialized")
          listeners foreach (_.onRequesterInitialized(link))
        }

        override def onRequesterConnected(link: DSLink) = {
          log.info("Requester connected")
          reqLink = link
          latch.countDown
          listeners foreach (_.onRequesterConnected(link))
        }

        override def onRequesterDisconnected(link: DSLink) = {
          log.warn("Requester disconnected from the broker")
          listeners foreach (_.onRequesterDisconnected(link))
        }
      })

      try {
        provider.start
      } catch {
        case NonFatal(e) => provider = null; throw e
      }

      Try(latch.await(DSAConfig.connectTimeout, TimeUnit.MILLISECONDS)) getOrElse {
        throw new Error(s"Cannot connect to DSA Broker")
      }

      DSAConnection(linkMode, provider, rspLink, reqLink)
    }
  }

  //$COVERAGE-ON$

  /**
    * Returns `true` if it is connected to the DSA broker.
    */
  def isConnected: Boolean = synchronized(provider != null)

  //$COVERAGE-OFF$
  /**
    * Closes the connection to the DSA broker.
    */
  def stop(): Unit = synchronized {
    if (isConnected) {
      provider.stop
      provider = null
      log.info("DSLinkProvider stopped")
    }
  }

  //$COVERAGE-ON$
}

/**
  * Provides alternative ways for creating a DSA Connector.
  */
object DSAConnector {

  /**
    * Creates a new DSAConnector, passing a list of arguments compatible with `DSLinkFactory.generate' method.
    **/
  def apply(args: String*): DSAConnector = apply(args.toIterable)

  /**
    * Creates a new DSAConnector, passing a list of arguments compatible with `DSLinkFactory.generate' method.
    **/
  def apply(args: Iterable[String]): DSAConnector = new DSAConnector(args)

  /**
    * Creates a new DSAConnector, passing the individual options. Only `brokerUrl` is mandatory.
    */
  def create(brokerUrl: String, token: Option[String] = None,
             nodesPath: Option[String] = None, keyPath: Option[String] = None,
             logLevel: Option[String] = None, logFilePath: Option[String] = None,
             dslinkJsonPath: Option[String] = None, dslinkName: Option[String] = None): DSAConnector = {

    def p(arg: Option[String], prefix: String) = arg.map(s => Array(prefix, s)).getOrElse(Array.empty[String])

    val args = Array("-b", brokerUrl) ++ p(token, "-t") ++ p(nodesPath, "-n") ++ p(keyPath, "-k") ++
      p(logLevel, "-l") ++ p(logFilePath, "-f") ++ p(dslinkJsonPath, "-d") ++ p(dslinkName, "--name")

    apply(args: _*)
  }
}