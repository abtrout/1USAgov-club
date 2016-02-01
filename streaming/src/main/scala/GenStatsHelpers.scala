package com.github.abtrout._1USAgov_club

import com.twitter.algebird.HLL
import com.twitter.algebird.HyperLogLog._
import com.twitter.algebird.HyperLogLogMonoid

trait GenStatsHelpers extends RequestHelpers {

  type GenStats = Tuple6[Long, Long, Int, HLL, HLL, HLL]

  private val HLL_BITSIZE = 12

  private def buildHLL(input: String): HLL = {
    val hll = new HyperLogLogMonoid(HLL_BITSIZE)
    hll(input.toCharArray.map(_.toByte))
  }

  def buildGenStats(r: Request): GenStats = {
    val countryHLL = buildHLL(r.countryCode)
    val agentHLL = buildHLL(r.userAgent)
    val urlHLL = buildHLL(r.destURL)

    (r.ts, r.ts, 1, urlHLL, countryHLL, agentHLL)
  }

  def combineGenStats(g: GenStats, h: GenStats): GenStats = {
    val (gtsMin, gtsMax, gtotal, gurlHLL, gcountryHLL, gagentHLL) = g
    val (htsMin, htsMax, htotal, hurlHLL, hcountryHLL, hagentHLL) = h

    (gtsMin min htsMin, gtsMax max htsMax, gtotal + htotal,
      gurlHLL + hurlHLL,
      gcountryHLL + hcountryHLL,
      gagentHLL + hagentHLL)
  }

  def prepareGenStatsRows(gs: GenStats) = {
    val (tsMax, tsMin, requests, urlHLL, countryHLL, agentHLL) = gs

    val day = roundDay(tsMax)
    val perSec: Int = requests / (tsMax - tsMin).toInt / 1000
    val countries = countryHLL.estimatedSize.toInt
    val agents = agentHLL.estimatedSize.toInt
    val urls = urlHLL.estimatedSize.toInt

    // TODO: For quantiles, need T-Digest.
    // TODO: For T-Digest, need T-Digest monoid...
    (day, tsMax, perSec, urls, countries, agents) 
  }

}
