package com.uralian.dsa.util

import org.dsa.iot.dslink.node.value.{Value, ValueType}
import org.dsa.iot.dslink.util.json.{JsonArray, JsonObject}

import scala.collection.JavaConverters.{asScalaBufferConverter, mapAsScalaMapConverter, seqAsJavaListConverter}

/**
  * Implicits and helper methods for DSA Value type.
  */
trait ValueUtils {

  /**
    * Extracts the data from a Value object.
    */
  def valueToAny(value: Value): Any = value.getType.toJsonString match {
    case ValueType.JSON_BOOL   => valueToBoolean(value)
    case ValueType.JSON_NUMBER => valueToNumber(value)
    case ValueType.JSON_BINARY => valueToBinary(value)
    case ValueType.JSON_STRING => valueToString(value)
    case ValueType.JSON_MAP    => valueToMap(value)
    case ValueType.JSON_ARRAY  => valueToList(value)
    case vt @ _                => throw new IllegalArgumentException(s"Invalid type: $vt")
  }

  /**
    * Converts a JsonArray instance into a scala List[Any].
    */
  def jsonArrayToList(arr: JsonArray): List[Any] = arr.getList.asScala.toList map {
    case x: JsonArray  => jsonArrayToList(x)
    case x: JsonObject => jsonObjectToMap(x)
    case x             => x
  }

  /**
    * Converts a JsonObject instance into a scala Map[String, Any].
    */
  def jsonObjectToMap(obj: JsonObject): Map[String, Any] = obj.getMap.asScala.toMap mapValues {
    case x: JsonArray  => jsonArrayToList(x)
    case x: JsonObject => jsonObjectToMap(x)
    case x             => x
  }

  /**
    * Converts a value into Value object.
    */
  def anyToValue(value: Any): Value = value match {
    case null                   => null
    case x: Value               => x
    case x: java.lang.Number    => numberToValue(x)
    case x: Boolean             => booleanToValue(x)
    case x: String              => stringToValue(x)
    case x: Array[Byte]         => binaryToValue(x)
    case x: Map[_, _]           => mapToValue(x.asInstanceOf[Map[String, _]])
    case x: java.util.Map[_, _] => mapToValue(x.asScala.toMap.asInstanceOf[Map[String, _]])
    case x: Seq[_]              => listToValue(x)
    case x: java.util.List[_]   => listToValue(x.asScala)
    case x @ _                  => new Value(x.toString)
  }

  /**
    * Converts a scala Seq[Any] instance into a JsonArray.
    */
  def listToJsonArray(ls: Seq[_]): JsonArray = {
    val elements = ls map {
      case x: Value               => valueToAny(x)
      case x: Seq[_]              => listToJsonArray(x)
      case x: java.util.List[_]   => listToJsonArray(x.asScala)
      case x: Map[_, _]           => mapToJsonObject(x.asInstanceOf[Map[String, Any]])
      case x: java.util.Map[_, _] => mapToJsonObject(x.asScala.toMap.asInstanceOf[Map[String, Any]])
      case x                      => x
    }
    new JsonArray(elements.asJava)
  }

  /**
    * Converts scala values into a JsonArray.
    */
  def jsonArray(values: Any*): JsonArray = listToJsonArray(values.toList)

  /**
    * Converts a scala Map[String, Any] instance into a JsonObject.
    */
  def mapToJsonObject(mp: Map[String, _]): JsonObject = {
    val elements = mp.mapValues {
      case x: Value               => valueToAny(x).asInstanceOf[Object]
      case x: Seq[_]              => listToJsonArray(x)
      case x: java.util.List[_]   => listToJsonArray(x.asScala)
      case x: Map[_, _]           => mapToJsonObject(x.asInstanceOf[Map[String, Any]])
      case x: java.util.Map[_, _] => mapToJsonObject(x.asScala.toMap.asInstanceOf[Map[String, Any]])
      case x                      => x.asInstanceOf[Object]
    }
    // due to issues with mutability, have to do it the long way instead of elements.toJava
    val m = new java.util.HashMap[String, Object]
    elements foreach {
      case (key, value) => m.put(key, value)
    }
    new JsonObject(m)
  }

  /**
    * Converts a list of tuples (String, Any) into a JsonObject.
    */
  def jsonObject(pairs: (String, Any)*): JsonObject = mapToJsonObject(pairs.toMap)

  /* implicit converters */

  implicit def valueToBoolean(v: Value): Boolean = v.getBool

  implicit def valueToNumber(v: Value): Number = v.getNumber

  implicit def valueToInt(v: Value): Int = v.getNumber.intValue

  implicit def valueToLong(v: Value): Long = v.getNumber.longValue

  implicit def valueToDouble(v: Value): Double = v.getNumber.doubleValue

  implicit def valueToFloat(v: Value): Float = v.getNumber.floatValue

  implicit def valueToBinary(v: Value): Array[Byte] = v.getBinary

  implicit def valueToString(v: Value): String = v.getString

  implicit def valueToMap(v: Value): Map[String, _] = jsonObjectToMap(v.getMap)

  implicit def valueToList(v: Value): List[_] = jsonArrayToList(v.getArray)

  implicit def booleanToValue(x: Boolean) = new Value(x)

  implicit def numberToValue(x: Number) = new Value(x)

  implicit def intToValue(x: Int) = numberToValue(x)

  implicit def longToValue(x: Long) = numberToValue(x)

  implicit def doubleToValue(x: Double) = numberToValue(x)

  implicit def floatToValue(x: Float) = numberToValue(x)

  implicit def binaryToValue(x: Array[Byte]) = new Value(x)

  implicit def stringToValue(x: String) = new Value(x)

  implicit def mapToValue(x: Map[String, _]) = new Value(mapToJsonObject(x))

  implicit def listToValue(x: Seq[_]) = new Value(listToJsonArray(x))

  /**
    * Resolves the value with unknown type.
    */
  private[dsa] def resolveUnknown(v: Value): Any = Option(v.getBool)
    .orElse(Option(v.getNumber))
    .orElse(Option(v.getBinary))
    .orElse(Option(v.getString))
    .orElse(Option(v.getMap) map jsonObjectToMap)
    .orElse(Option(v.getArray) map jsonArrayToList)
    .orNull
}