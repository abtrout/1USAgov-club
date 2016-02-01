package com.github.abtrout._1USAgov_club

import argonaut._, Argonaut._

import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

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

    val conf = new SparkConf().setAppName("StreamingStats")
      .set("spark.cassandra.connection.host", cassandraHost)

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

    val hostCountCols = SomeColumns("day", "hour", "minute", "hostname", "total", "unique")

    requests.flatMap(buildCounts)
      .reduceByKey(combineCounts)
      .map(prepareHostCountRows)
      .saveToCassandra("oneusa", "host_counts", hostCountCols)

    // We track a few Top K queries:
    //  * ~~Top K most frequently seen (inboundHost, outboundHost) pairs~~
    //  * Top K most frequently seen inboundHosts 
    //  * Top K most freqneutly seen outboundHosts
    //
    // These are built from the past (batchTime * 10) minutes, and are written to the `topk`
    // table in Cassandra, every (batchTime * 10) seconds. Currently, K = 25.

    val topkCols = SomeColumns("day", "ts", "topkin", "topkout")

    requests.map(buildSketches)
      .reduceByWindow(combineSketches, Minutes(60), Seconds(batchTime * 5))
      .map(prepareTopKRows)
      .saveToCassandra("oneusa", "topk", topkCols)

    // Our last query of interest tracks general stats:
    // * counts of unique outgoing hosts, user agents, and country codes
    // * requests per second (and quartiles for requests per second)
    //
    // These are sliding window computations over the last 30 minutes of data, updated
    // every (batchSize * 10) seconds.

    val genStatsCols = SomeColumns("day", "ts", "reqps", "govurls", "countries", "agents")

    requests.map(buildGenStats)
      .reduceByWindow(combineGenStats, Minutes(60), Seconds(batchTime * 5))
      .map(prepareGenStatsRows)
      .saveToCassandra("oneusa", "gen_stats", genStatsCols)

    ssc.start()
    ssc.awaitTermination()
  }

  def getEnv(name: String, default: String) = sys.env.get(name).getOrElse(default)
}
