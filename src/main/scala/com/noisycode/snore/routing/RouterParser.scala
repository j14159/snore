package com.noisycode.snore.routing

import scala.util.parsing.combinator._

sealed trait PathPart

case class Separator() extends PathPart
case class StaticSegment(s: String) extends PathPart
case class DynamicSegment(label: String) extends PathPart

case class Path(parts: List[PathPart])

object RouteParser extends RegexParsers {
  def static: Parser[StaticSegment] = """([a-zA-Z0-9]+)""".r ^^ { s  => StaticSegment(s) }
  def dynamic: Parser[DynamicSegment] = """:([a-zA-Z0-9]+)""".r ^^ { label => DynamicSegment(label.tail) }
  def separator: Parser[Separator] = """/""".r ^^ { _ => Separator() }

  def full: Parser[Path] = "/" ~> repsep((dynamic | static), "/") ^^ { p => Path(p) }

  def parsePath(p: String) = parseAll(full, p)
}
