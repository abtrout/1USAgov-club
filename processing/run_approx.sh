#!/usr/bin/env bash

#export SPARK_HOME=""
#export SPARK_URL=""
#export CHECKPOINT_URL=""
#export KAFKA_TOPIC=""
#export CONSUMER_GROUP=""
#export ZOOKEEPER_QUORUM=""
#export CASSANDRA_HOST=""

# Spark tuning:
#export BLOCK_INTERVAL=200
#export BATCH_INTERVAL=2
#export MAX_RATE=1000
#export NUM_THREADS=1
#export NUM_STREAMS=3

$SPARK_HOME/bin/spark-submit \
  --master $SPARK_URL \
  --executor-memory 8192M \
  --driver-memory 8192M \
  --conf "spark.serializer=org.apache.spark.serializer.KryoSerializer" \
  --conf "spark.executor.extraJavaOptions=-XX:+UseCompressedOops" \
  --class "com.github.abtrout._1USAgov_club.ApproxStats" \
  target/scala-2.10/1USAgov.club-streaming.jar
