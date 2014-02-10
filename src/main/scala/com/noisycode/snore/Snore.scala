package com.noisycode.snore

import com.typesafe.config.ConfigFactory

import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext

object SnoreContext {
  val parallelism = ConfigFactory.load().getInt("snore.parallelism")
  val ec = ExecutionContext.fromExecutor(new ForkJoinPool(parallelism))
}

object Snore {
  val conf = ConfigFactory.load()

  def run(routes: Map[String, RestPipeline.Pipeline[_]]): Unit = {
    val r = new routing.Router(routes)
    val server = new netty.HttpServer(r, conf)
    server.run()
  }
}
