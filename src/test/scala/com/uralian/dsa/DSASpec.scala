package com.uralian.dsa

import org.dsa.iot.dslink.link.Linkable
import org.dsa.iot.dslink.node.actions.{Action, ActionResult, EditorType, Parameter}
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.{Node, NodeBuilder, Writable}
import org.dsa.iot.dslink.util.json.JsonObject
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import scala.collection.JavaConverters._

/**
  * DSA helpers test suite.
  */
class DSASpec extends AbstractUnitSpec with MockitoSugar {

  private val linkable = mock[Linkable]

  "RichNode" should {
    val node = new Node("abc", null, linkable)
    "handle attributes" in {
      node.attributes shouldBe empty
      node.setAttribute("aaa", 123)
      node.attributes shouldBe Map("aaa" -> 123)
    }
    "handle configs" in {
      node.configurations shouldBe empty
      node.setConfig("xyz", true)
      node.configurations shouldBe Map("xyz" -> true)
    }
    "handle roConfigs" in {
      node.roConfiguration shouldBe empty
      node.setRoConfig("zzz", 1.25)
      node.roConfiguration shouldBe Map("zzz" -> 1.25)
    }
    "handle interfaces" in {
      node.interfaces shouldBe empty
      node.setInterfaces("blah1|blah2")
      node.interfaces shouldBe Set("blah1", "blah2")
    }
    "handle children" in {
      node.children shouldBe empty
      val child = new Node("child", null, linkable)
      node.addChild(child)
      node.children shouldBe Map("child" -> child)
    }
  }

  "RichNodeBuilder" should {
    val nb = mock[NodeBuilder]
    "build node with options" in {
      nb display "Child" attributes("a" -> 1, "b" -> 2) interfaces ("i1", "i2", "i3")
      nb config("x" -> true, "y" -> false) roConfig("rx" -> 0, "ry" -> 1)
      nb valueType ValueType.STRING value "aaaa" hidden true profile "child" meta "meta"
      nb serializable true writable Writable.NEVER
      nb.action(_ => {})
      verify(nb) setDisplayName "Child"
      verify(nb) setAttribute("a", 1)
      verify(nb) setAttribute("b", 2)
      verify(nb) setInterfaces "i1|i2|i3"
      verify(nb) setConfig("x", true)
      verify(nb) setConfig("y", false)
      verify(nb) setRoConfig("rx", 0)
      verify(nb) setRoConfig("ry", 1)
      verify(nb) setValueType ValueType.STRING
      verify(nb) setValue "aaaa"
      verify(nb) setHidden true
      verify(nb) setMetaData "meta"
      verify(nb) setSerializable true
      verify(nb) setWritable Writable.NEVER
      verify(nb) setAction any[Action]
    }
  }

  "createAction" should {
    "build an action from components" in {
      import ValueType._
      var side: Int = 0
      val a = createAction(_ => {
        side = 10
      }, STRING("abc"), STRING("xyz"))
      a.parameters.headOption.value shouldBe Map("name" -> "abc", "type" -> "string")
      a.results.headOption.value shouldBe Map("name" -> "xyz", "type" -> "string")
      a.invoke(null)
      side shouldBe 10
    }
  }

  "RichAction" should {
    val action = mock[Action]
    "extract action parameters" in {
      when(action.getParams).thenReturn(jsonArray("a", "b"))
      action.parameters shouldBe List("a", "b")
    }
    "extract action results" in {
      when(action.getColumns).thenReturn(jsonArray("c", "d"))
      action.results shouldBe List("c", "d")
    }
  }

  "RichParameter" should {
    "build parameter with options" in {
      val param = mock[Parameter]
      param default "12345" description "My password" editorType EditorType.PASSWORD placeHolder "pwd"
      param meta Map("a" -> 1, "b" -> 2)
      verify(param) setDefaultValue "12345"
      verify(param) setDescription "My password"
      verify(param) setEditorType EditorType.PASSWORD
      verify(param) setPlaceHolder "pwd"
      verify(param) setMetaData any[JsonObject]
      (param ~ mock[Parameter]) shouldBe a[List[_]]
    }
  }

  "ENUMS" should {
    "build ENUM ValueType from Scala Enumeration" in {
      object MyEnum extends Enumeration {
        val Morning, Day, Evening, Night = Value
      }
      val vType = ENUMS(MyEnum)
      vType.getEnums.asScala.toSet shouldBe Set("Morning", "Day", "Evening", "Night")
    }
  }

  "RichValueType" should {
    "produce parameters" in {
      import ValueType._
      STRING("a").getName shouldBe "a"
      STRING("a").getType shouldBe STRING
    }
  }

  "RichActionResult" should {
    "provide parameter extractors" in {
      val ar = mock[ActionResult]
      when(ar.getParameter("abc")).thenReturn("123")
      when(ar.getParameter("xyz")).thenReturn("")

      ar.getParam[String]("abc") shouldBe "123"
      verify(ar).getParameter("abc")
      an[IllegalArgumentException] should be thrownBy ar.getParam[String]("xyz", !_.isEmpty)
    }
  }

  "Having" should {
    "retain value and produce side effects for Function1" in {
      var side: Int = 0
      val result = 5.having { x => side = x * x }
      result shouldBe 5
      side shouldBe 25
    }
    "retain value and produce side effects for Function0" in {
      var side: Int = 0
      val result = 5.having {
        side = 121
      }
      result shouldBe 5
      side shouldBe 121
    }
  }

}
