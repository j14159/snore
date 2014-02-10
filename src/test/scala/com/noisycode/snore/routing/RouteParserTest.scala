package com.noisycode.snore.routing

import org.scalatest._

class RouteParserTest extends FlatSpec with Matchers {
  "RouteParser.parsePath" should "parse correctly" in {
    val path1 = "/all/static"
    val path2 = "/some/:dynamic/parts"
    val path3 = "/" 

    val res1 = RouteParser.parsePath(path1).get
    val res2 = RouteParser.parsePath(path2).get
    val res3 = RouteParser.parsePath(path3).get

    res1 shouldBe Path(List(StaticSegment("all"), StaticSegment("static")))
    res2 should be (Path(List(StaticSegment("some"), DynamicSegment("dynamic"), StaticSegment("parts"))))
  }
}
