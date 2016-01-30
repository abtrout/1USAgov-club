package com.github.abtrout._1USAgov_club

import com.twitter.algebird._
import com.twitter.algebird.CMSHasherImplicits._

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import com.datastax.spark.connector.{SomeColumns}
import com.datastax.spark.connector.streaming._

import argonaut._, Argonaut._
import java.net.URI
import scala.util.{Try,Success}

object StreamingStats extends StatsHelpers
  with SketchHelpers {

  def getEnv(name: String, default: String) = sys.env.get(name).getOrElse(default)

  def main(args: Array[String]) = {

    // TODO: gracefully pull these from args?
    val batchTime = getEnv("BATCH_SECONDS", "2").toInt
    val numThreads = getEnv("NUM_THREADS", "1").toInt
    val topics = getEnv("KAFKA_TOPIC", "default")
    val topicMap = topics.split(",").map((_, numThreads)).toMap
    val zkQuorum = getEnv("ZOOKEEPER_QUORUM", "localhost:2181")
    val cassandraHost = getEnv("CASSANDRA_HOST", "localhost")
    val checkpointURL = getEnv("CHECKPOINT_URL", "hdfs://localhost:9000/checkpoint")

    val conf = new SparkConf()
      .setAppName("StreamingStats")
      .set("spark.closure.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.cassandra.connection.host", cassandraHost)

    val ssc = new StreamingContext(conf, Seconds(batchTime))
    ssc.checkpoint(checkpointURL)

    val stream = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap)
    val requests = stream.flatMap(_._2.decodeOption[Request])

    // Straightforward counting to (accurately) track total/unique traffic
    // per domain. We'd use HLL to track uniques, but 1USAgov already sends
    // us [anonymized data with] a boolean indicating if the visit is unique.
    // (We use HLL to track some other things below instead.)
    //
    // Inbound and outbound hosts get lumped together here and computed stats
    // end up together in the same `host_counts` table in Cassandra.
    //
    val hostCounts = requests.flatMap({ r =>
        // We keep track of minutely traffic by rounding our timestamps down to the
        // minute, and incrementing the respective minutely counters in Cassandra.
        val ts = roundTimestamp(System.currentTimeMillis / 1000)
        val unique = if(r.knownUser == 1) 0 else 1

        val inbound = ((ts, parseHost(r.sourceURL)), (1, unique))
        val outbound = ((ts, parseHost(r.destURL)), (1, unique))
        List(inbound, outbound)
      })
      .reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2))
      .map { x =>
        val (ts, host) = x._1
        val (total, unique) = x._2
        (ts, host, total, unique)
      }

    hostCounts.saveToCassandra("oneusa", "host_counts",
      SomeColumns("ts", "hostname", "total", "unique"))

    // We track a few Top K queries:
    //  * Top K most frequently seen (inboundHost, outboundHost) pairs
    //  * Top K most frequently seen inboundHosts 
    //  * Top K most freqneutly seen outboundHosts
    //
    // These are built from the past (batchTime * 10) minutes, and are written to the `topk`
    // table in Cassandra, every (batchTime * 10) seconds. Currently, K = 25.
    val top25 = requests.map(buildSketches)
      .reduceByWindow(combineSketches, Minutes(batchTime * 10), Seconds(batchTime * 5))
      .map { x =>
        val (ts, cmsIn, cmsOut, cmsPair) = x
        val leadersIn = getLeaders(cmsIn)
        val leadersOut = getLeaders(cmsOut)
        val leadersPair = getLeaders(cmsPair)

        // TODO: make sure that Request turns timestamps in seconds to milliseconds.
        // Then we won't have to multiple by 1000 here.
        (roundDay(ts), ts * 1000, leadersIn, leadersOut, leadersPair)
      }
    
    top25.saveToCassandra("oneusa", "topk",
      SomeColumns("day", "ts", "topkIn", "topkOut", "topkPair"))

    ssc.start()
    ssc.awaitTermination()
  }
}

trait SketchHelpers {

  def buildSketches(r: Request) = {
    val hostIn = parseHost(r.sourceURL)
    val hostOut = parseHost(r.destURL)
    val hostPair = s"$hostIn $hostOut"

    val cmsIn = TopPctCMS.monoid[String](0.01, 0.0001, 1, 0.001)
    val cmsOut = TopPctCMS.monoid[String](0.01, 0.0001, 1, 0.001)
    val cmsPair = TopPctCMS.monoid[String](0.01, 0.0001, 1, 0.001)

    (r.ts, cmsIn.create(hostIn), cmsOut.create(hostOut), cmsPair.create(hostPair))
  }

  type TopKQueries = Tuple4[Long, TopPctCMSMonoid, TopPctCMSMonoid, TopPctCMSMonoid]

  def combineSketches(x: TopKQueries, y: TopKQueries): TopKQueries = {
    val (xts, xIn, xOut, xPair) = x
    val (yts, yIn, yOut, yPair) = y

    (xts max yts, xIn ++ yIn, xOut ++ yOut, xPair ++ yPair)
  }

  def getLeaders(cms: TopPctCMSMonoid, k: Int = 25) = {
    cms.heavyHitters
      .map(x => (x, cms.frequency(x).estimate))
      .toSeq.sortBy(_._2).reverse.slice(0, k)
      .toMap
  }
}

trait StatsHelpers {

  // Want to round this down to 5 minute chunks instead, and make sure our
  // query INCREMENTS the counters we've implemented in cassandra!
  def roundTimestamp(ts: Long): Long = (ts - (ts % 60)) * 1000
  def roundDay(ts: Long): Long = (ts - (ts % 86400)) * 1000

  // Incoming data includes two URLs: the referring URL, the destination URL.
  // For most of our stats, we're only interested in hostnames, though.
  def parseHost(url: String): String = {
    // Catch a special case ("direct" visit) up front.
    if(url == "direct") return "DIRECT" 

    // Some data is inevitably messed up. We catch [URISyntax]Exceptions with a
    // generic default value. Also, we strip leading `www`.
    Try(new URI(url).getHost) match {
      case Success(host) if host != null => host.replaceFirst("^www.", "")
      case _ => "INVALID"
    }
  }
}
