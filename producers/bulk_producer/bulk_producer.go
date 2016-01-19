package main

import (
	"bufio"
	"flag"
	"github.com/shopify/sarama"
	"log"
	"os"
	"strings"
	"time"
)

var (
	brokers  = flag.String("brokers", os.Getenv("KAFKA_PEERS"), "The kafka brokers to connect to, as a comma separated list")
	topic    = flag.String("topic", os.Getenv("KAFKA_TOPIC"), "The Kafka topic to publish messages to")
	filename = flag.String("file", os.Getenv("INPUT_FILE"), "File to read raw traffic from")
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
		line := scanner.Text()
		producer.Input() <- &sarama.ProducerMessage{
			Topic: *topic,
			Value: sarama.StringEncoder(line),
		}
	}
}

func newAsyncProducer(brokerList []string) sarama.AsyncProducer {

	// For the access log, we are looking for AP semantics, with high throughput.
	// By creating batches of compressed messages, we reduce network I/O at a cost of more latency.
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForLocal
	config.Producer.Compression = sarama.CompressionSnappy
	config.Producer.Flush.Frequency = 500 * time.Millisecond

	producer, err := sarama.NewAsyncProducer(brokerList, config)
	if err != nil {
		log.Fatalln("Failed to start Sarama producer:", err)
	}

	// We will just log to STDOUT if we're not able to produce messages.
	// Note: messages will only be returned here after all retry attempts are exhausted.
	go func() {
		for err := range producer.Errors() {
			log.Println("Failed to write entry:", err)
		}
	}()

	return producer
}
