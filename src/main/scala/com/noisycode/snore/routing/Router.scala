package com.noisycode.snore.routing

import com.noisycode.snore.RestPipeline

class Router(routeMap: Map[String, RestPipeline.Pipeline[_]]) {

  val routes = routeMap.map {
    case (path, pipeline) => RouteParser.parsePath(path).get -> pipeline
  }
  
  /**
    * Given a requested path, return the route that first matches it along with any bindings
    * if the matched path had dynamic parts.
    */
  def routeForPath(path: String): Option[(Map[String, String], RestPipeline.Pipeline[_])] = {
    val parsed = RouteParser.parsePath(path).get
    val sameLength = routes.filter(_._1.parts.length == parsed.parts.length)
    sameLength.map {
      case (Path(parts), pipeline) =>
        val b = bindings(parts, parsed.parts, Map())
        (b, pipeline)
    }.filter(_._1.isDefined).map(tup => tup._1.get -> tup._2).headOption
  }

  /**
    * Recurse through the established path and the submitted one, determining bindings along the way.
    * @param path the path registered in this router.
    * @param the requested path to check for matches.
    */
  def bindings(path: List[PathPart], check: List[PathPart], binds: Map[String, String]): Option[Map[String, String]] = {
    (path, check) match {
      case (List(), List()) => Some(binds)
      case (StaticSegment(a) :: aa, StaticSegment(b) :: bb) if a == b => bindings(aa, bb, binds)
      case (DynamicSegment(a) :: aa, StaticSegment(b) :: bb) => bindings(aa, bb, binds + (a -> b))
      case _ => None
    }
  }
}
