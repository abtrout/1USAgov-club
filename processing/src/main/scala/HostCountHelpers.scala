package com.github.abtrout._1USAgov_club

import com.datastax.spark.connector.{SomeColumns}

trait HostCountHelpers extends RequestHelpers {

  type Counts = Tuple2[Int, Int]
  type Host = Tuple2[String, Long]
  type HostCounts = Tuple2[Host, Counts]

  val minutelyCountCols = SomeColumns("day", "minute", "hostname", "total", "unique")
  val hourlyCountCols = SomeColumns("day", "hour", "hostname", "total", "unique")
  val dailyCountCols = SomeColumns("day", "hostname", "total", "unique")

  def buildCounts(r: Request): List[HostCounts] = {
    val unique = if(r.knownUser == 1) 0 else 1

    ((r.sourceURL, r.ts), (1, unique)) ::
      ((r.destURL, r.ts), (1, unique)) :: Nil
  }

  def toMinutes(hc: HostCounts): HostCounts = {
    val (host, ts) = hc._1
    ((host, roundMinute(ts)), hc._2)
  }

  def toHours(hc: HostCounts): HostCounts = {
    val (host, ts) = hc._1
    ((host, roundHour(ts)), hc._2)
  }

  def toDays(hc: HostCounts): HostCounts = {
    val (host, ts) = hc._1
    ((host, roundDay(ts)), hc._2)
  }

  def combineCounts(x: Counts, y: Counts) = (x._1 + y._1, x._2 + y._2)

  def prepareMinutelyCountRows(hc: HostCounts) = {
    val ((hostname, ts), (total, unique)) = hc
    val (day, _, minute, _) = splitTimestamp(ts)

    (day, minute, hostname, total, unique)
  }

  def prepareHourlyCountRows(hc: HostCounts) = {
    val ((hostname, ts), (total, unique)) = hc
    val (day, hour, _, _) = splitTimestamp(ts)

    (day, hour, hostname, total, unique)
  }

  def prepareDailyCountRows(hc: HostCounts) = {
    val ((hostname, ts), (total, unique)) = hc
    val day = roundDay(ts)

    (day, hostname, total, unique)
  }
}
