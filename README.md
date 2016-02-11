## 1USAgov club

is a real-time web analytics platform for government URL redirects, created as part of my [Insight Data Engineering](insightdataengineering.com) fellowship. 1.USA.gov is a URL shortening service for government links. USA.gov releases JSON data for all requests it handles, and 1USAgov club processes it and displays computed statistics on a frontend.

1USAgov club's data pipeline is powered by the following technologies:

* [Kafka](http://kafka.apache.org/) for ingestion
* [Cassandra](http://cassandra.apache.org/) for storage
* [Spark-streaming](http://spark.apache.org/streaming/) for real-time processing with [Twitter Algebird](https://github.com/twitter/algebird)

For more information: play around with [the site](https://1USAgov.club), check out my [demo slides](https://1USAgov.club/slides), or explore this repository!
