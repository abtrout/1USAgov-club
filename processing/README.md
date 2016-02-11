## Real-time processing

Real-time processing is handled by spark-streaming with twitter algebird for HyperLogLog and CountMinSketch computations over 5 minute windows. Computed stats are written to Cassandra.

Data can be pulled from Kafka _much_ faster than I can process it, so setting `spark.streaming.receiver.maxRate=MAX_RATE` was crucial for achieving stable processing. I was able to stably process 30,000 requests per second with 4 second micro-batches.
