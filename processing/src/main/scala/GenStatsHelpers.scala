package com.github.abtrout._1USAgov_club

import com.twitter.algebird.HLL
import com.twitter.algebird.HyperLogLog._
import com.twitter.algebird.HyperLogLogMonoid

import com.datastax.spark.connector.SomeColumns

trait GenStatsHelpers extends RequestHelpers {

  val genStatsCols = SomeColumns("day", "ts", "reqps", "govurls", "countries")

  type GenStats = Tuple5[Long, Long, Int, HLL, HLL]

  private val HLL_BITSIZE = 12

  private def buildHLL(input: String): HLL = {
    val hll = new HyperLogLogMonoid(HLL_BITSIZE)
    hll(input.toCharArray.map(_.toByte))
  }

  def buildGenStats(r: Request): GenStats = {
    val urlHLL = buildHLL(r.destURL)
    val countryHLL = buildHLL(r.countryCode)

    (r.ts, r.ts, 1, urlHLL, countryHLL)
  }

  def combineGenStats(g: GenStats, h: GenStats): GenStats = {
    val (gtsMin, gtsMax, gtotal, gurlHLL, gcountryHLL) = g
    val (htsMin, htsMax, htotal, hurlHLL, hcountryHLL) = h

    (gtsMin min htsMin, gtsMax max htsMax, gtotal + htotal,
      gurlHLL + hurlHLL, gcountryHLL + hcountryHLL)
  }

  def prepareGenStatsRows(gs: GenStats) = {
    val (tsMin, tsMax, requests, urlHLL, countryHLL) = gs
    val day = roundDay(tsMax)

    val perSec: Int = ((requests.toDouble / (tsMax - tsMin)) * 1000).toInt
    val countries = countryHLL.estimatedSize.toInt
    val urls = urlHLL.estimatedSize.toInt

    (day, tsMax, perSec, urls, countries)
  }
}
