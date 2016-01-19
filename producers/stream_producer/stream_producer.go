package main

import (
	"bufio"
	"flag"
	"github.com/shopify/sarama"
	"log"
	"net/http"
	"os"
	"strings"
)

var (
	brokers = flag.String("brokers", os.Getenv("KAFKA_PEERS"), "The kafka brokers to connect to, as a comma separated list")
	topic   = flag.String("topic", os.Getenv("KAFKA_TOPIC"), "The Kafka topic to publish messages to")
)

func main() {
	flag.Parse()

	if *brokers == "" {
		flag.PrintDefaults()
		os.Exit(1)
	}

	brokerList := strings.Split(*brokers, ",")
	producer := newSyncProducer(brokerList)

	resp, _ := http.Get("http://developer.usa.gov/1usagov")
	reader := bufio.NewReader(resp.Body)

	for {
		line, err := reader.ReadBytes('\n')
		if err != nil {
			log.Println("Failed to ReadBytes:", err)
		} else if len(line) > 2 {
			producer.SendMessage(&sarama.ProducerMessage{
				Topic: *topic,
				Value: sarama.StringEncoder(line),
			})
		}
	}
}

func newSyncProducer(brokerList []string) sarama.SyncProducer {

	// For the data collector, we are looking for strong consistency semantics.
	// Because we don't change the flush settings, sarama will try to produce messages
	// as fast as possible to keep latency low.
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForAll
	config.Producer.Retry.Max = 10

	// On the broker side, you may want to change the following settings to get
	// stronger consistency guarantees:
	// - For your broker, set `unclean.leader.election.enable` to false
	// - For the topic, you could increase `min.insync.replicas`.
	producer, err := sarama.NewSyncProducer(brokerList, config)
	if err != nil {
		log.Fatalln("Failed to start Sarama producer:", err)
	}

	return producer
}
