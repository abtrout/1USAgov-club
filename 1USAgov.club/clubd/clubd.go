package main

import (
	"flag"
	"github.com/gocql/gocql"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"os"
	"strings"
)

var (
	session *gocql.Session
	keyspace = flag.String("keyspace", os.Getenv("CASSANDRA_KEYSPACE"), "Cassandra keyspace")
	hosts = flag.String("hosts", os.Getenv("CASSANDRA_HOSTS"), "Cassandra hosts in a comma-separated list")
)

func main() {
	flag.Parse()
	session = startCassandraSession()
	defer session.Close()

	router := httprouter.New()
	router.GET("/stats/counts", HostCounts)
	router.GET("/stats/topk", TopLeaders)

	// TODO: finish implementing routes
	//router.GET("/stats/general", GeneralStats)
	//router.POST("/filter", TrafficFilter)

	log.Fatal(http.ListenAndServe(":8080", router))
}

// Initializes our Cassandra session.
func startCassandraSession() *gocql.Session {
	cluster := gocql.NewCluster(strings.Split(*hosts, ",")...)
	cluster.Keyspace = *keyspace
	session, err := cluster.CreateSession()

	if err != nil {
		log.Fatal("Failed to start cassandra session: ", err)
	}

	return session
}
