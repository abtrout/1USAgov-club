package com.github.abtrout._1USAgov_club

import com.datastax.spark.connector.{SomeColumns}

trait HostCountHelpers extends RequestHelpers {

  type Counts = Tuple2[Int, Int]
  type Host = Tuple2[String, Long]
  type HostCounts = Tuple2[Host, Counts]

  val hostCountCols = SomeColumns("day", "minute", "hostname", "total", "unique")

  def buildCounts(r: Request): List[HostCounts] = {
    val minute = roundMinute(r.ts)
    val unique = if(r.knownUser == 1) 0 else 1

    ((r.sourceURL, minute), (1, unique)) ::
      ((r.destURL, minute), (1, unique)) :: Nil
  }

  def combineCounts(x: Counts, y: Counts) = {
    (x._1 + y._1, x._2 + y._2)
  }

  def prepareHostCountRows(hc: HostCounts) = {
    val ((hostname, ts), (total, unique)) = hc
    val (day, _, minute, _) = splitTimestamp(ts)

    (day, minute, hostname, total, unique)
  }
}
