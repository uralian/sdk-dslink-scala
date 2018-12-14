package com.uralian.dsa.util

import java.io.{File, PrintWriter}

import com.uralian.dsa._

import scala.io.Source

/**
  * Utilities for running examples and integration tests.
  */
object TestUtils {

  /**
    * Creates a new DSAConnector.
    */
  def createConnector(brokerUrl: String) = {
    val dslinkJson = copyDslinkJson("/examples/dslink.json.template", "dslink", ".json")
    val nodesJsonPath = dslinkJson.getParent + "/nodes.json"
    DSAConnector.create(brokerUrl = brokerUrl, dslinkJsonPath = Some(dslinkJson.getPath), nodesPath = Some(nodesJsonPath))
  }

  /**
    * Copies a resource file into a temporary file and returns it descriptor. The temporary file is to be deleted after
    * the JVM shutfown.
    *
    * @param resource
    * @param prefix
    * @param suffix
    * @return
    */
  private def copyDslinkJson(resource: String, prefix: String = "dslink", suffix: String = ".json") = {
    val source = Source.fromInputStream(getClass.getResourceAsStream(resource))

    val dslinkFile = File.createTempFile(prefix, suffix)
    dslinkFile.deleteOnExit

    val target = new PrintWriter(dslinkFile)
    source.getLines foreach target.println
    target.close

    dslinkFile
  }
}
