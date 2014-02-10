package com.noisycode.snore.netty

import com.typesafe.config.Config

import com.noisycode.snore.routing.Router

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.channel.socket.nio.NioServerSocketChannel

/*
 * This whole file is based on https://github.com/netty/netty/tree/4.0/example/src/main/java/io/netty/example/http/helloworld
 * because it's been a while since I touched Netty and that was some version of 3.x
 */

class HttpServer(router: Router, conf: Config) {
  val listenOnPort = conf.getInt("snore.listen-on-port")
  val listenOnAddress = conf.getString("snore.listen-on-addr")
  val workerThreadCount = conf.getInt("snore.netty-worker-threads")

  def run(): Unit = {
    val bossGroup = new NioEventLoopGroup(1)
    val workGroup = new NioEventLoopGroup(workerThreadCount)

    val b = new ServerBootstrap
    b.group(bossGroup, workGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new HttpServerInit(router))

    val ch = b.bind(listenOnAddress, listenOnPort).sync().channel()
  }
}

class HttpServerInit(router: Router) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline
    p.addLast("codec", new HttpServerCodec)
    p.addLast("handler", new HttpHandler(router))
  }
}

