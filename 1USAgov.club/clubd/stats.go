package main

import (
	"fmt"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"strings"
)

func InboundCounts(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	table := "inbound_counts"
	hostnames := []string{"direct", "facebook.com", "t.co"}

	jsonHandler(w, r, getCounts(table, hostnames))
}

func OutboundCounts(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	table := "outbound_counts"
	hostnames := []string{"nasa.gov", "nsa.gov", "cia.gov"}

	jsonHandler(w, r, getCounts(table, hostnames))
}

type HostCount struct {
	Timestamp int64  `json:"ts"`
	Hostname  string `json:"hostname"`
	Total     int    `json:"total"`
	Unique    int    `json:"unique"`
}

func getCounts(table string, hostnames []string) []HostCount {
	var count HostCount
	counts := []HostCount{}

	q := "SELECT ts, hostname, total, unique FROM %s WHERE hostname IN ? LIMIT 25"
	iter := session.Query(fmt.Sprintf(q, table), hostnames).Iter()

	for iter.Scan(&count.Timestamp, &count.Hostname, &count.Total, &count.Unique) {
		counts = append(counts, count)
	}

	return counts
}

type KthLeader struct {
	Source string `json:"source"`
	Dest   string `json:"dest"`
	Total  int    `json:"total"`
}

func TopLeaders(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	var ts int64
	var tmp map[string]int

	err := session.Query("SELECT ts, topk FROM topk LIMIT 1").Scan(&ts, &tmp)
	if err != nil {
		log.Println("Failed to get Top K: ", err)
	}

	topK := []KthLeader{}
	for hosts, total := range tmp {
		hosts := strings.Split(hosts, " ")
		if len(hosts) == 2 {
			topK = append(topK, KthLeader{hosts[0], hosts[1], total})
		}
	}

	jsonHandler(w, r, topK)
}
