package com.github.abtrout._1USAgov_club

trait HostCountHelpers extends RequestHelpers {

  type Counts = Tuple2[Int, Int]
  type Host = Tuple2[String, Long]
  type HostCounts = Tuple2[Host, Counts]

  def buildCounts(r: Request): List[HostCounts] = {
    val unique = if(r.knownUser == 1) 0 else 1
    val minute = roundMinute(r.ts)

    ((r.sourceURL, minute), (1, unique)) ::
      ((r.destURL, minute), (1, unique)) :: Nil
  }

  def combineCounts(x: Counts, y: Counts) = (x._1 + y._1, x._2 + y._2)

  def prepareHostCountRows(hc: HostCounts) = {
    val ((hostname, minute), (total, unique)) = hc
    val (day, hour, _, _) = splitTimestamp(minute)
    (day, hour, minute, hostname, total, unique)
  }
}
