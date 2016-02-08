package main

import (
	"bufio"
	"flag"
	"github.com/shopify/sarama"
	"log"
	"os"
	"strings"
)

var (
	batchSize = flag.Int("batch-size", 5000, "Number of messages to send per batch")
	filename  = flag.String("file", os.Getenv("INPUT_FILE"), "File to read raw traffic from")
	brokers   = flag.String("brokers", os.Getenv("KAFKA_PEERS"), "The kafka brokers to connect to, as a comma separated list")
	topic     = flag.String("topic", os.Getenv("KAFKA_TOPIC"), "The Kafka topic to publish messages to")
)

func main() {
	flag.Parse()

	if *brokers == "" {
		flag.PrintDefaults()
		os.Exit(1)
	}

	brokerList := strings.Split(*brokers, ",")
	producer := newAsyncProducer(brokerList)

	f, err := os.Open(*filename)
	defer f.Close()

	if err != nil {
		log.Fatal("Could not open raw file:", err)
	}

	scanner := bufio.NewScanner(f)
	scanner.Split(bufio.ScanLines)

	for scanner.Scan() {
		producer.Input() <- &sarama.ProducerMessage{
			Topic: *topic,
			Value: sarama.StringEncoder(scanner.Text()),
		}
	}
}

func newAsyncProducer(brokerList []string) sarama.AsyncProducer {
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForLocal
	config.Producer.Compression = sarama.CompressionSnappy
	config.Producer.Flush.MaxMessages = *batchSize

	producer, err := sarama.NewAsyncProducer(brokerList, config)
	if err != nil {
		log.Fatalln("Failed to start Sarama producer:", err)
	}

	go func() {
		for err := range producer.Errors() {
			log.Println("Failed to write entry:", err)
		}
	}()

	return producer
}
