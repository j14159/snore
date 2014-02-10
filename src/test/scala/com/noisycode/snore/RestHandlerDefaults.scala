package com.noisycode.snore

import io.netty.handler.codec.http.HttpMethod

import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class RestHandlerDefaults extends FlatSpec with Matchers {
  val testGet = Request(Map(), null)
  val testPut = Request(Map(), null)

  implicit val ec = ExecutionContext.global

  class TestPipeline extends PipelineHelpers[String] {
    val route = RestPipeline.init("Hello, world")
      .acceptContent(contentAllowed)
      .provideContent(contentProvided)

    def handlePut = syncHandle {
      case (r, s) => Ok(s"The init message was ${s}")
    }

    def getText = asyncHandle((r, s) => Future.successful(Ok("You got it")))

    def getJson = syncHandle((r, s) => Ok("No json for you"))

    def contentAllowed = contentMap(Map("text/plain" -> handlePut))

    def contentProvided = contentMap(Map(
      "application/json" -> getJson,
      "text/plain" -> getText))
  }

  "A simple " should "respond with defaults" in {
    RestPipeline.init(0).allowedMethods(testGet, 0) should be 
    ((Set(HttpMethod.GET, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.POST), Request, 0))

    val t = new TestPipeline
    println(t.route)
    println(s"content allowed is ${t.contentAllowed}")

    val res1 = Await.result(t.route.contentAllowed(testGet, "")._1("text/plain")(testPut, "a"), 1 second)
    res1 should be (Ok("The init message was a"))
  }
}
