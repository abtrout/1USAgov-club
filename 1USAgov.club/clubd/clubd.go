package main

import (
	"github.com/gocql/gocql"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
)

var (
	session *gocql.Session
)

func main() {
	session = startCassandraSession()
	defer session.Close()

	router := httprouter.New()
	router.GET("/stats/inbound", InboundCounts)
	router.GET("/stats/outbound", OutboundCounts)
	router.GET("/stats/topk", TopLeaders)

	// TODO: finish implementing routes
	//router.GET("/stats/general", GeneralStats)
	//router.POST("/filter", TrafficFilter)

	log.Fatal(http.ListenAndServe(":8080", router))
}

// Initializes our Cassandra session.
func startCassandraSession() *gocql.Session {
	cluster := gocql.NewCluster("davidc-storage1", "davidc-storage2", "davidc-storage3", "davidc-storage4")
	cluster.Keyspace = "oneusa"
	cluster.Consistency = gocql.Quorum

	session, err := cluster.CreateSession()
	if err != nil {
		log.Fatal("Failed to start cassandra session: ", err)
	}

	return session
}

// Initializes our Kafka consumer.
//func kafkaInit() {}
