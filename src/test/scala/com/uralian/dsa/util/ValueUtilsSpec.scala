package com.uralian.dsa.util

import com.uralian.dsa.AbstractUnitSpec
import org.dsa.iot.dslink.node.value.{Value, ValueType}

import scala.collection.JavaConverters._

/**
  * Value utilities test suite.
  */
class ValueUtilsSpec extends AbstractUnitSpec with ValueUtils {

  "list<->JsonArray conversion" should {
    "handle simple lists" in forAll(gen.scalarLists) { x =>
      jsonArrayToList(listToJsonArray(x)) shouldBe x
      jsonArrayToList(jsonArray(x: _*)) shouldBe x
    }
    "handle nested lists and maps" in forAll(gen.anyLists) { x =>
      jsonArrayToList(listToJsonArray(x)) shouldBe x
    }
    "handle lists with Value items" in forAll(gen.scalarLists) { x =>
      val y = jsonArrayToList(listToJsonArray(x map anyToValue))
      (y zip x) foreach {
        case (a, b) => a should equal(b)
      }
    }
    "handle lists with java list items" in forAll(gen.scalarJavaLists) { x =>
      val y = x.asScala.toList
      jsonArrayToList(listToJsonArray(List(x))) shouldBe jsonArrayToList(listToJsonArray(List(y)))
    }
    "handle lists with java map items" in forAll(gen.scalarJavaMaps) { x =>
      val y = x.asScala.toMap
      jsonArrayToList(listToJsonArray(List(x))) shouldBe jsonArrayToList(listToJsonArray(List(y)))
    }
  }

  "map<->JsonObject conversion" should {
    "handle simple maps" in forAll(gen.scalarMaps) { x =>
      jsonObjectToMap(mapToJsonObject(x)) shouldBe x
      jsonObjectToMap(jsonObject(x.toSeq: _*)) shouldBe x
    }
    "handle nested lists and maps" in forAll(gen.anyMaps) { x =>
      jsonObjectToMap(mapToJsonObject(x)) shouldBe x
    }
    "handle maps with Value items" in forAll(gen.scalarMaps) { x =>
      val y = jsonObjectToMap(mapToJsonObject(x mapValues anyToValue))
      x.size shouldBe y.size
      val z = x foreach {
        case (k, v1) => y(k) should equal(v1)
      }
    }
    "handle maps with java list items" in forAll(gen.scalarJavaLists) { x =>
      val y = x.asScala.toList
      jsonObjectToMap(mapToJsonObject(Map("a" -> x))) shouldBe jsonObjectToMap(mapToJsonObject(Map("a" -> y)))
    }
    "handle maps with java map items" in forAll(gen.scalarJavaMaps) { x =>
      val y = x.asScala.toMap
      jsonObjectToMap(mapToJsonObject(Map("a" -> x))) shouldBe jsonObjectToMap(mapToJsonObject(Map("a" -> y)))
    }
  }

  "any<->Value conversion" should {
    "handle nulls" in {
      anyToValue(null) shouldBe null
    }
    "handle booleans" in forAll { (x: Boolean) =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.BOOL
      v.getBool shouldBe x
      valueToAny(v) shouldBe x
    }
    "handle numbers" in forAll { (x: Number) =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.NUMBER
      v.getNumber shouldBe x
      valueToInt(v) shouldBe x.intValue
      valueToLong(v) shouldBe x.longValue
      valueToDouble(v) shouldBe x.doubleValue
      valueToFloat(v) shouldBe x.floatValue
      valueToAny(v) shouldBe x
    }
    "handle integers" in forAll { (x: Int) =>
      val v = intToValue(x)
      v.getType shouldBe ValueType.NUMBER
      valueToInt(v) shouldBe x
    }
    "handle longs" in forAll { (x: Long) =>
      val v = longToValue(x)
      v.getType shouldBe ValueType.NUMBER
      valueToLong(v) shouldBe x
    }
    "handle doubles" in forAll { (x: Double) =>
      val v = doubleToValue(x)
      v.getType shouldBe ValueType.NUMBER
      valueToDouble(v) shouldBe x
    }
    "handle floats" in forAll { (x: Float) =>
      val v = floatToValue(x)
      v.getType shouldBe ValueType.NUMBER
      valueToFloat(v) shouldBe x
    }
    "handle strings" in forAll { (x: String) =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.STRING
      v.getString shouldBe x
      valueToAny(v) shouldBe x
    }
    "handle binaries" in forAll { (x: Array[Byte]) =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.BINARY
      v.getBinary shouldBe x
      valueToAny(v) shouldBe x
    }
    "handle lists" in forAll(gen.anyLists) { x =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.ARRAY
      jsonArrayToList(v.getArray) shouldBe x
      valueToAny(v) shouldBe x
    }
    "handle maps" in forAll(gen.anyMaps) { x =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.MAP
      jsonObjectToMap(v.getMap) shouldBe x
      valueToAny(v) shouldBe x
    }
    "handle java lists" in forAll(gen.scalarJavaLists) { x =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.ARRAY
      jsonArrayToList(v.getArray) shouldBe x.asScala
      valueToAny(v) shouldBe x.asScala
    }
    "handle java maps" in forAll(gen.scalarJavaMaps) { x =>
      val v = anyToValue(x)
      v.getType shouldBe ValueType.MAP
      jsonObjectToMap(v.getMap) shouldBe x.asScala
      valueToAny(v) shouldBe x.asScala
    }
    "handle arbitrary types" in {
      val v = anyToValue(Some("abc"))
      v.getType shouldBe ValueType.STRING
      v.getString shouldBe "Some(abc)"
      valueToAny(v) shouldBe "Some(abc)"
    }
    "fail on unknown value type" in {
      val typeField = classOf[Value].getDeclaredField("type")
      typeField.setAccessible(true)

      val v = new Value(true)

      typeField.set(v, ValueType.ENUM)
      an[IllegalArgumentException] should be thrownBy valueToAny(v)

      typeField.set(v, ValueType.DYNAMIC)
      an[IllegalArgumentException] should be thrownBy valueToAny(v)
    }
  }

  "resolveUnknown" should {
    "handle booleans" in forAll(valueGen.bools) { v =>
      resolveUnknown(v) shouldBe v.getBool
    }
    "handle numbers" in forAll(valueGen.numbers) { v =>
      resolveUnknown(v) shouldBe v.getNumber
    }
    "handle strings" in forAll(valueGen.strings) { v =>
      resolveUnknown(v) shouldBe v.getString
    }
    "handle binaries" in forAll(valueGen.binary) { v =>
      resolveUnknown(v) shouldBe v.getBinary
    }
    "handle arrays" in forAll(valueGen.arrays) { v =>
      resolveUnknown(v) shouldBe jsonArrayToList(v.getArray)
    }
    "handle maps" in forAll(valueGen.maps) { v =>
      resolveUnknown(v) shouldBe jsonObjectToMap(v.getMap)
    }
    "handle nulls" in {
      val v = new Value(null.asInstanceOf[String])
      resolveUnknown(v) shouldBe null.asInstanceOf[Any]
    }
  }
}