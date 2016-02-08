package com.github.abtrout._1USAgov_club

import com.twitter.algebird.HLL
import com.twitter.algebird.HyperLogLog._
import com.twitter.algebird.HyperLogLogMonoid

import com.datastax.spark.connector.SomeColumns

trait GenStatsHelpers extends RequestHelpers {

  type GenStats = Tuple3[Long, HLL, HLL]

  val genStatsCols = SomeColumns("day", "ts", "govurls", "countries")

  private val HLL_BITSIZE = 12

  private def buildHLL(input: String): HLL = {
    val hll = new HyperLogLogMonoid(HLL_BITSIZE)
    hll(input.toCharArray.map(_.toByte))
  }

  def buildGenStats(r: Request): GenStats = {
    val urlHLL = buildHLL(r.destURL)
    val countryHLL = buildHLL(r.countryCode)

    (r.ts, urlHLL, countryHLL)
  }

  def combineGenStats(g: GenStats, h: GenStats): GenStats = {
    val (gtsMax, gurlHLL, gcountryHLL) = g
    val (htsMax, hurlHLL, hcountryHLL) = h

    (gtsMax max htsMax, gurlHLL + hurlHLL, gcountryHLL + hcountryHLL)
  }

  def prepareGenStatsRows(gs: GenStats) = {
    val (tsMax, urlHLL, countryHLL) = gs

    val day = roundDay(tsMax)
    val countries = countryHLL.estimatedSize.toInt
    val urls = urlHLL.estimatedSize.toInt

    (day, tsMax, urls, countries)
  }
}
