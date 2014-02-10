package com.noisycode.snore

import io.netty.handler.codec.http.HttpMethod

object Example extends App {


  override def main(args: Array[String]): Unit = {
    Snore.run(Map(
      "/" -> StaticEndpoints.route,
      "/:name" -> HelloEndpoint.route))

  }
}

object StaticEndpoints extends PipelineHelpers[String] {
  val route = RestPipeline.init("Hello, world")
    .withMethods(methods)
    .acceptContent(contentMap(Map("text/plain" -> put)))
    .provideContent(contentMap(Map("text/plain" -> get)))

  def methods = methodSet(Set(HttpMethod.GET, HttpMethod.PUT))

  def get = syncHandle {
    case (request, state) => Ok(s"The initial state was ${state}")
  }

  def put = syncHandle {
    case (request, state) => Ok(s"I'd change the state here if I cared.")
  }
}

object HelloEndpoint extends PipelineHelpers[Int] {

  val route = RestPipeline.init(0)
    .withMethods(myMethods)
    .provideContent(contentMap(Map("text/plain" -> get)))

  def myMethods = methodSet(Set(HttpMethod.GET))

  def get = syncHandle {
    case (r @ Request(bindings, _), s) =>
      val name = bindings("name")
      Ok(s"Hello, ${name}")
  }
}
