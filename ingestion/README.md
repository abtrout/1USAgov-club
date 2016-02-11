## Ingestion

1USAgov club relies on a 4-node Kafka cluster for ingestion.

These golang scripts use `shopify/sarama` to send it data:

* the `stream_producer` loads data from USA.gov's live stream
* the `bulk_producer` loads archived data in asynchronous batches

_Note:_ the live stream only generates around 5 requests per second, so I'm actually not using it!
