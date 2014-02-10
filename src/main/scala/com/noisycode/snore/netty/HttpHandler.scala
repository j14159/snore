package com.noisycode.snore.netty

import com.noisycode.snore._
import com.noisycode.snore.routing.Router

import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.handler.codec.http._

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future, Promise }

class HttpHandler(router: Router) extends SimpleChannelInboundHandler[HttpRequest] {
  implicit val ec = SnoreContext.ec

  def response(status: HttpResponseStatus) = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest): Unit = {
    router.routeForPath(msg.getUri) match {
      case Some((bindings, pipeline)) =>
        processPipeline(Request(bindings, msg), pipeline).future.recover {
          case t: Throwable =>
            t.printStackTrace  //TODO:  slf4j
            response(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }.map {resp => 
          ctx.write(resp)
          ctx.flush
          ctx.close.addListener(ChannelFutureListener.CLOSE)
        }
      case None => 
        ctx.write(response(HttpResponseStatus.NOT_FOUND))
    }
  }

  def processPipeline[S](req: Request, pipeline: RestPipeline.Pipeline[S]): Promise[HttpResponse] = {
    val s = pipeline.initialState
    val (methods, r2, s2) = pipeline.allowedMethods(req, s)
    val ret = if (!methods.contains(req.baseReq.getMethod))
      Future.successful(response(HttpResponseStatus.METHOD_NOT_ALLOWED))
    else
      pipeline.resourceExists(r2, s2).flatMap {
        case (false, r3, s3) if req.baseReq.getMethod() == HttpMethod.GET =>
          Future.successful(response(HttpResponseStatus.NOT_FOUND))
        case (_, r3, s3) => pipeline.isAuthorized(r2, s2).flatMap {
          case (false, _, _) => Future.successful(response(HttpResponseStatus.UNAUTHORIZED))
          case (true, r4, s4) => handleMethod(r4, s4, pipeline)
        }
      }

    Promise[HttpResponse]().completeWith(ret)
  }

  def handleMethod[S](req: Request, state: S, pipeline: RestPipeline.Pipeline[S]): Future[HttpResponse] = {
    req.baseReq.getMethod() match {
      case HttpMethod.GET => handleGet(req, state, pipeline)
      case HttpMethod.PUT | HttpMethod.POST => handleCreate(req, state, pipeline)
      case HttpMethod.DELETE => throw new Exception("Delete not implemented, need callbacks implemented first.")
    }
  }

  def complete(c: Complete) = {
    val resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(c.responseCode), Unpooled.wrappedBuffer(c.body))
    c.headers.map { case (k, v) => resp.headers.set(k, v) }
    resp
  }

  def handleGet[S](r: Request, s: S, pipeline: RestPipeline.Pipeline[S]): Future[HttpResponse] = {
    acceptOk(r, s, pipeline) match {
      case (None, _, _) => Future.successful(response(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE))
      case (Some(contentType), r2, s2) => 
        val (m, r3, s3) = pipeline.contentProvided(r, s)
        m(contentType)(r3, s3)
          .map(resp => resp.complete.copy(headers = resp.complete.headers + (HttpHeaders.Names.CONTENT_TYPE -> contentType)))
          .map(complete)
    }
  }

  def handleCreate[S](r: Request, s: S, pipeline: RestPipeline.Pipeline[S]): Future[HttpResponse] = {
    contentOk(r, s, pipeline) match {
      case (None, _, _) => Future.successful(response(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE))
      case (Some(contentType), r2, s2) => 
        val (m, r3, s3) = pipeline.contentAllowed(r, s)
        m(contentType)(r3, s3).map(_.complete).map(complete)
    }
  }

  /**
   * Right now this is pretty strict, no wildcards yet except for * / * and completely ignores parameters/charsets.
   */
  def acceptOk[S](req: Request, state: S, pipeline: RestPipeline.Pipeline[S]): (Option[String], Request, S) = {
    val acceptedTypes = HttpHeaders.getHeader(req.baseReq, HttpHeaders.Names.ACCEPT).split(",").toList.map(_.split(";").head)
    val (map, r2, s2) = pipeline.contentProvided(req, state)
    val targetType = acceptedTypes match {
      case List("*/*") => 
        Some(map.head._1)
      case other =>
        other.filter(t => map.get(t).isDefined).headOption
    }
    (targetType, r2, s2)
  }

  /**
   * Right now this is pretty strict, no wildcards yet except for * / * and completely ignores parameters/charsets.
   */
  def contentOk[S](req: Request, state: S, pipeline: RestPipeline.Pipeline[S]): (Option[String], Request, S) = {
    val acceptedTypes = HttpHeaders.getHeader(req.baseReq, HttpHeaders.Names.CONTENT_TYPE).split(",").toList.map(_.split(";").head)
    val (map, r2, s2) = pipeline.contentAllowed(req, state)
    val targetType = acceptedTypes match {
      case List("*/*") => 
        Some(map.head._1)
      case other =>
        other.filter(t => map.get(t).isDefined).headOption
    }
    (targetType, r2, s2)

  }
}
