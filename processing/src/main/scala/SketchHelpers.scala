package com.github.abtrout._1USAgov_club

import com.twitter.algebird.{TopCMS, TopPctCMS}
import com.twitter.algebird.CMSHasherImplicits._

import com.datastax.spark.connector.SomeColumns

trait SketchHelpers extends RequestHelpers {

  type TopKQueries = Tuple3[Long, TopCMS[String], TopCMS[String]]

  val topkCols = SomeColumns("day", "ts", "topkin", "topkout")

  private val eps: Double = 0.0001
  private val delta: Double = 0.01
  private val topPct: Double = 0.01
  private val seed: Int = 369928005

  def buildSketches(r: Request) = {
    val cmsIn = TopPctCMS.monoid[String](eps, delta, seed, topPct)
    val cmsOut = TopPctCMS.monoid[String](eps, delta, seed, topPct)

    (r.ts, cmsIn.create(r.sourceURL), cmsOut.create(r.destURL))
  }

  def combineSketches(x: TopKQueries, y: TopKQueries): TopKQueries = {
    val (xts, xIn, xOut) = x
    val (yts, yIn, yOut) = y

    (xts max yts, xIn ++ yIn, xOut ++ yOut)
  }

  def prepareTopKRows(ks: TopKQueries) = {
    val (ts, cmsIn, cmsOut) = ks
    (roundDay(ts), ts, getLeaders(cmsIn), getLeaders(cmsOut))
  }

  private def getLeaders(cms: TopCMS[String], k: Int = 10) = {
    cms.heavyHitters
      .map(x => (x, cms.frequency(x).estimate)).toSeq
      .sortBy(_._2).reverse.slice(0, k)
      .toMap
  }
}
