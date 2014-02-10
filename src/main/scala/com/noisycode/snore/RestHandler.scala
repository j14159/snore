package com.noisycode.snore

import io.netty.handler.codec.http.{ HttpMethod, HttpRequest }

import scala.concurrent.{ ExecutionContext, Future, Promise }

sealed trait Response {
  def complete(): Complete
}

case class Request(
  bindings: Map[String, String],
  baseReq: HttpRequest
) {
  lazy val content: Array[Byte] = {
    throw new Exception("not implemented")
  }
}

case class Complete(responseCode: Int, headers: Map[String, String], body: Array[Byte]) extends Response {
  def complete = this
}

case class Ok(body: String) extends Response {
  def complete = Complete(200, Map(), body.getBytes("UTF-8"))
}

case class Redirect(where: String) extends Response {
  def complete = Complete(301, Map("Location" -> where), Array())
}

/**
  * Helper functions to help you avoid putting types everywhere.
  */
trait PipelineHelpers[S] {
  import RestPipeline._

  def methodSet(ms: Set[HttpMethod]): Handler[S, Set[HttpMethod]] = {
    def f: Handler[S, Set[HttpMethod]] = { case (r, s) => (ms, r, s) }
    f
  }

  def contentMap(m: Map[String, LastHandler[S]]): Handler[S, Map[String, LastHandler[S]]] = {
    def f: Handler[S, Map[String, LastHandler[S]]] = ((r, s) => (m, r, s))
    f
  }

  def syncHandle(f: (Request, S) => Response): LastHandler[S] = { 
    def x: LastHandler[S] = { case (r, s) => Future.successful(f(r, s)) }
    x
  }

  def asyncHandle(f: LastHandler[S]) = f
}

object RestPipeline {
  type Handler[S, R] = (Request, S) => (R, Request, S)
  type AsyncHandler[S, R] = (Request, S) => Future[(R, Request, S)]
  type LastHandler[S] = (Request, S) => Future[Response]


  def init[S](s: S) = Pipeline(s, 
    defaultAllowed[S], 
    defaultContentProvided[S], 
    defaultContentAllowed[S],
    defaultAuthorized[S],
    defaultExists[S])

  case class Pipeline[S](
    initialState: S,
    allowedMethods: Handler[S, Set[HttpMethod]],
    contentProvided: Handler[S, Map[String, LastHandler[S]]],
    contentAllowed: Handler[S, Map[String, LastHandler[S]]],
    isAuthorized: AsyncHandler[S, Boolean],
    resourceExists: AsyncHandler[S, Boolean]
  ) {
    def withMethods(f: Handler[S, Set[HttpMethod]]) = this.copy(allowedMethods = f)

    def authorizeWith(f: Handler[S, Boolean]) = {
      val handler: AsyncHandler[S, Boolean] = { case (r, s) => Future(f(r, s))(SnoreContext.ec) }
      this.copy(isAuthorized = handler)
    }

    def existsWith(f: Handler[S, Boolean]) = {
      val handler: AsyncHandler[S, Boolean] = { case (r, s) => Future(f(r, s))(SnoreContext.ec) }
      this.copy(resourceExists = handler)
    }

    def acceptContent(f: Handler[S, Map[String, LastHandler[S]]]) = 
      this.copy(contentAllowed = f)

    def provideContent(f: Handler[S, Map[String, LastHandler[S]]]) = this.copy(contentProvided = f)
  } 

  def defaultAllowed[S](req: Request, state: S): (Set[HttpMethod], Request, S) =
    (Set(HttpMethod.GET, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.POST), req, state)

  def defaultProvider[S](r: Request, s: S): Response = Ok("Default placeholder")

  def defaultContentProvided[S](req: Request, state: S): (Map[String, LastHandler[S]], Request, S) = {
    val f: LastHandler[S] = { case (r, s) => Future.successful(Ok("wat")) }
    (Map("text/plain" -> f), req, state)
  }

  def defaultContentAllowed[S](req: Request, s: S) = {
    val f: LastHandler[S] = { case (r, s) => Future.successful(Ok("wat")) }
    (Map("text/plain" -> f), req, s)
  }

  def defaultAuthorized[S](r: Request, s: S) = Future.successful((true, r, s))
  def defaultExists[S](r: Request, s: S) = Future.successful((true, r, s))
}
