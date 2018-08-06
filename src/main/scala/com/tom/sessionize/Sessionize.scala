package com.tom.sessionize

import scala.io.Source
import scala.util.Try

object Sessionize {
  def main(args: Array[String]): Unit = {
    println("DeviceId\tFrom\tTo\tActivity")
    Source
      .fromInputStream(System.in)
//      .fromResource("sessionize/input.txt")
      .getLines()
      .map(_.split(Array(' ', '\t')).filter(_.length > 0)) // split into the parts and make sure to get rid of all whitespace
      .filter(_.length == 3) // get rid of invalid lines
      .flatMap(it => Try(it(1).toInt).toOption.map(ts => Input(it(0), ts, it(2)))) // make sure timestamp is an int
      .toList // materialize the whole input
      .groupBy(_.id)
      .flatMap { case (key, entries) =>
        entries
          .sortBy(_.timestamp)
          .foldLeft((List.empty[Output], Option.empty[Input])) { (acc, n) =>
            acc match {
              case (l, None) => // first entry, so list is empty
                (l, Some(n))
              case (l, Some(last)) if last.timestamp + 2 < n.timestamp => // the last activity timed out
                (l :+ Output(key, last.timestamp, last.timestamp + 2, last.activity), Some(n))
              case (l, Some(last)) =>
                (l :+ Output(key, last.timestamp, n.timestamp, last.activity), Some(n)) // channel was switched
            }
          }._1

      }.foreach(println)
  }

}

case class Input(id: String, timestamp: Int, activity: String)

case class MapEntry(start: Int, end: Int, activity: String)

case class Output(id: String, from: Int, to: Int, activity: String) {
  private val ids = id.padTo("DeviceId\t".length, " ").mkString
  private val froms = from.toString.padTo("From\t".length, " ").mkString
  private val tos = to.toString.padTo("To\t".length, " ").mkString
  private val activitys = activity.padTo("Activity\t".length, " ").mkString

  override def toString: String = s"$ids\t$froms\t$tos\t$activitys"
}
