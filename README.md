# sdk-dslink-scala

![build status](https://travis-ci.com/uralian/sdk-dslink-scala.svg?branch=master)
![coverage status](https://coveralls.io/repos/github/uralian/sdk-dslink-scala/badge.svg?branch=master)

DSLink Scala SDK

## Features

- Provides DSAConnector and DSAConnection classes as a communication facade.
- Tracks DSA connection lifecycle through DSAEventListener interface.
- Implements DSAHelper object for basic DSA operations, such as invoke, subscribe, set, etc.
- Exposes DSA API through Reactive Streams paradigm.
- Implements fluent Scala layer to facilitate operations with DSA artifacts.
- Recognizes all existing Node API data types.

## Usage

### DSAConnector

You can create a DSAConnector passing individual settings or the entire argument list:

```scala
val brokerUrl = ...
val configPath = ...
val connector = DSAConnector("-b", brokerUrl, "-d", configPath)
```

or

```scala
def main(args: Array[String]): Unit = {
  val connector = DSAConnector(args)
}
```

You can then add listeners and initiate a new connection do DSA:

```scala
connector.addListener(new DSAEventListener {
  override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)
})
val connection = connector.start(RESPONDER)
val root = connection.responderLink.getNodeManager.getSuperRoot
root createChild "counter" valueType ValueType.NUMBER value 0 build ()
...
connector.stop
```

### DSAHelper

Most DSAHelper methods require implicit Requester or Responder object. You can use the ones provided by
DSAConnection:

```scala
val connection = connector start LinkMode.DUAL
implicit val requester = connection.requester
implicit val responder = connection.responder
```

Example calling `DSA Invoke` method:

```scala
DSAHelper invoke (path, key1 -> value1, key2 -> value2) subscribe (
	onNext = event => println("Event received: " + event),
    onError = err => println("Error occurred: " + err),
    onCompleted = () => println("Stream closed, no more data")
)
```

Example calling `DSA Subscribe` on multiple nodes and merging the results:

```scala
val cpu = DSAHelper watch "/downstream/System/CPU_Usage"
val mem = DSAHelper watch "/downstream/System/Memory_Usage"
cpu merge mem subscribe { sv =>
	println(sv.getPath + " : " + sv.getValue)
}
```

Note that you can subscribe to the same path multiple times using `watch` without raising an error.

### Node Builder

This SDK also provides an extension to the Java NodeBuilder class to expose a simple DSL.

Example creating new nodes:

```scala
parentNode createChild "changeValue" display "Update Value" action (
	createAction(
		parameters = List("value" -> ValueType.NUMBER),
    	handler = result => {
      		val value = result.getParameter("value").getNumber
			DSAHelper updateNode "/output/value" -> value
		}
	)
) build
```
