package main

import (
	"bufio"
	"encoding/json"
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
		}

		if isJSON(line) {
			producer.SendMessage(&sarama.ProducerMessage{
				Topic: *topic,
				Value: sarama.StringEncoder(line),
			})
		}
	}
}

func isJSON(bytes []byte) bool {
	var tmp map[string]interface{}
	return json.Unmarshal(bytes, &tmp) == nil
}

func newSyncProducer(brokerList []string) sarama.SyncProducer {
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForAll
	config.Producer.Retry.Max = 10

	producer, err := sarama.NewSyncProducer(brokerList, config)
	if err != nil {
		log.Fatalln("Failed to start Sarama producer:", err)
	}

	return producer
}
