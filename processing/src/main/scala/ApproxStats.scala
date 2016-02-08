package com.github.abtrout._1USAgov_club

import com.twitter.algebird._
import com.twitter.algebird.CMSHasherImplicits._

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._

import argonaut._, Argonaut._

object ApproxStats extends GenStatsHelpers
  with HostCountHelpers
  with SketchHelpers {

  private val blockInterval = getEnv("BLOCK_INTERVAL", "200").toInt
  private val batchInterval = getEnv("BATCH_INTERVAL", "2").toInt
  private val maxRate = getEnv("MAX_RATE", "5000").toInt
  private val numThreads = getEnv("NUM_THREADS", "1").toInt
  private val numStreams = getEnv("NUM_STREAMS", "3").toInt
  private val checkpointURL = getEnv("CHECKPOINT_URL", "hdfs://localhost:9000/checkpoint")
  private val zkQuorum = getEnv("ZOOKEEPER_QUORUM", "localhost:2181")
  private val topics = getEnv("KAFKA_TOPIC", "rawdata")
  private val cassandraHost = getEnv("CASSANDRA_HOST", "localhost")
  private val cgroup = getEnv("CONSUMER_GROUP", "oneusa")

  def main(args: Array[String]) = {
    val conf = new SparkConf()
      .set("spark.streaming.blockInterval", s"$blockInterval")
      .set("spark.streaming.receiver.maxRate", s"$maxRate")
      .set("spark.cassandra.connection.host", cassandraHost)
      .setAppName("ApproxStats")

    val ssc = new StreamingContext(conf, Seconds(batchInterval))
    ssc.checkpoint(checkpointURL)

    val topicMap = topics.split(",").map((_, numThreads)).toMap
    val stream = ssc.union((1 to numStreams).map(i =>
      KafkaUtils.createStream(ssc, zkQuorum, cgroup, topicMap)))

    val requests = stream.flatMap(_._2.decodeOption[Request]).cache()

    // Straightforward counting to (accurately) track total/unique traffic
    // per domain. We'd use HLL to track uniques, but 1USAgov already sends
    // us [anonymized data with] a boolean indicating if the visit is unique.
    // (We use HLL to track some other things below instead.)
    //
    // Inbound and outbound hosts get lumped together here and computed stats
    // end up together in the same `host_counts` table in Cassandra.

    requests.flatMap(buildCounts)
      .reduceByKey(combineCounts)
      .map(prepareHostCountRows)
      .saveToCassandra("oneusa", "host_counts", hostCountRows)

    // We track Top K queries for most frequently seen inbound/outbound hosts.
    //
    // Currently K = 10 and these are computed every `batchInterval * 5` seconds over
    // the past `batchInterval * 5` minutes of requests. Note that this is a windowed
    // computation; both the window length and sliding interval must be multiples
    // of batchInterval!

    requests.map(buildSketches)
      .reduceByWindow(combineSketches, Minutes(5), Seconds(batchInterval * 2))
      .map(prepareTopKRows)
      .saveToCassandra("oneusa", "topk", topkCols)

    // Lastly, we track the unique number of country codes and government URLs
    // seen over the past 5 minutes, updated every `batchInterval * 2` seconds.

    requests.map(buildGenStats)
      .reduceByWindow(combineGenStats, Minutes(5), Seconds(batchInterval * 2))
      .map(prepareGenStatsRows)
      .saveToCassandra("oneusa", "gen_stats", genStatsCols)

    ssc.start()
    ssc.awaitTermination()
  }

  def getEnv(name: String, default: String) = {
    sys.env.get(name).getOrElse(default)
  }
}
