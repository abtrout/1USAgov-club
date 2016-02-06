package com.github.abtrout._1USAgov_club

import com.twitter.algebird._
import com.twitter.algebird.CMSHasherImplicits._

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._

import argonaut._, Argonaut._

object StreamingStats extends GenStatsHelpers
  with HostCountHelpers
  with SketchHelpers {

  private val batchTime = getEnv("BATCH_SECONDS", "2").toInt
  private val numThreads = getEnv("NUM_THREADS", "1").toInt
  private val numStreams = getEnv("NUM_STREAMS", "1").toInt
  private val topics = getEnv("KAFKA_TOPIC", "default")
  private val cgroup = getEnv("CONSUMER_GROUP", "oneusagov")
  private val zkQuorum = getEnv("ZOOKEEPER_QUORUM", "localhost:2181")
  private val cassandraHost = getEnv("CASSANDRA_HOST", "localhost")
  private val checkpointURL = getEnv("CHECKPOINT_URL", "hdfs://localhost:9000/checkpoint")

  def main(args: Array[String]) = {
    val conf = new SparkConf()
      .set("spark.cassandra.connection.host", cassandraHost)
      .setAppName("StreamingStats")

    val ssc = new StreamingContext(conf, Seconds(batchTime))
    ssc.checkpoint(checkpointURL)

    val topicMap = topics.split(",").map((_, numThreads)).toMap
    val stream = ssc.union((1 to numStreams).map(i =>
      KafkaUtils.createStream(ssc, zkQuorum, cgroup, topicMap)))

    val requests = stream.flatMap(_._2.decodeOption[Request])

    // Straightforward counting to (accurately) track total/unique traffic
    // per domain. We'd use HLL to track uniques, but 1USAgov already sends
    // us [anonymized data with] a boolean indicating if the visit is unique.
    // (We use HLL to track some other things below instead.)
    //
    // Inbound and outbound hosts get lumped together here and computed stats
    // end up together in the same `host_counts` table in Cassandra.
    val counts = requests.flatMap(buildCounts)

    counts.map(toMinutes)
      .reduceByKey(combineCounts)
      .map(prepareMinutelyCountRows)
      .saveToCassandra("oneusa", "counts_minute", minutelyCountCols)

    counts.map(toHours)
      .reduceByKey(combineCounts)
      .map(prepareHourlyCountRows)
      .saveToCassandra("oneusa", "counts_hour", hourlyCountCols)

    counts.map(toDays)
      .reduceByKey(combineCounts)
      .map(prepareDailyCountRows)
      .saveToCassandra("oneusa", "counts_day", dailyCountCols)

    // We track Top K queries for most frequently seen inbound/outbound hosts.
    //
    // Currently K = 10 and these are computed every `batchTime * 5` seconds over
    // the past `batchTime * 5` minutes of requests. Note that this is a windowed
    // computation; both the window length and sliding interval must be multiples
    // of batchTime!

    requests.map(buildSketches)
      .reduceByWindow(combineSketches, Minutes(batchTime * 5), Seconds(batchTime * 5))
      .map(prepareTopKRows)
      .saveToCassandra("oneusa", "topk", topkCols)

    // Lastly, we track some general stats:
    // * counts of unique government hosts and country codes (using HLL)
    // * average requests per second
    //
    // These are windowed computations with similar bounds as above.

    requests.map(buildGenStats)
      .reduceByWindow(combineGenStats, Minutes(batchTime * 5), Seconds(batchTime * 5))
      .map(prepareGenStatsRows)
      .saveToCassandra("oneusa", "gen_stats", genStatsCols)

    ssc.start()
    ssc.awaitTermination()
  }

  def getEnv(name: String, default: String) = sys.env.get(name).getOrElse(default)
}
