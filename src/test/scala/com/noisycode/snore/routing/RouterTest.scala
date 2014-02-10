package com.noisycode.snore.routing

import io.netty.handler.codec.http.HttpMethod

import com.noisycode.snore._

import org.scalatest._

import scala.concurrent.Future

class RouterTest extends FlatSpec with Matchers {
  val p1 = new TestPipeline1
  val p2 = new TestPipeline2

  "Router.routeForPath" should "retrieve the correct handler" in {
    val r = new Router(Map(
      "/test1" -> p1.route,
      "/test2/:dynamic" -> p2.route))

    r.routeForPath("/test1") should be (Some(Map(), p1.route))
    r.routeForPath("/test2/ok") should be (Some(Map("dynamic" -> "ok"), p2.route))
    r.routeForPath("/test2") should be (None)
    r.routeForPath("/test") should be (None)
  }

  "Router.routeForPath" should "retrieve the correct handler with same length paths" in {
    val r = new Router(Map(
      "/all/static/parts" -> p1.route,
      "/some/:dynamic/parts" -> p2.route))

    r.routeForPath("/all/static/parts") should be (Some(Map(), p1.route))
    r.routeForPath("/some/wat/parts") should be (Some(Map("dynamic" -> "wat"), p2.route))
    r.routeForPath("/all/different/parts") should be (None)
    r.routeForPath("/some/dynamic/bits") should be (None)
  }
}

class TestPipeline1 extends PipelineHelpers[String] {
  val route = RestPipeline.init("Hello, world")
    .acceptContent(contentAllowed)
    .provideContent(contentProvided)

  val handlePut = syncHandle {
    case (r, s) => Ok(s"The init message was ${s}")
  }

  val getText = asyncHandle((r, s) => Future.successful(Ok("You got it")))

  val getJson = syncHandle((r, s) => Ok("No json for you"))

  val contentAllowed = contentMap(Map("text/plain" -> handlePut))
  val contentProvided = contentMap(Map(
    "application/json" -> getJson,
    "text/plain" -> getText))
}

class TestPipeline2 extends PipelineHelpers[String] {

  val route = RestPipeline.init("Test pipeline2")
//    .withMethods(methods)
  
//  val methods = allowMethods(Set(HttpMethod.GET))

}
