package main

import (
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type HostCount struct {
	Timestamp int64  `json:"ts"`
	Hostname  string `json:"hostname"`
	Total     int    `json:"total"`
	Unique    int    `json:"unique"`
}

func HostCounts(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	var count HostCount
	counts := []HostCount{}

	query, _ := url.ParseQuery(r.URL.RawQuery)
	hostnames, ok := query["hostnames"]
	if ok && len(hostnames) > 0 {
		hostnames = strings.Split(hostnames[0], ",")
	} else {
		hostnames = []string{"facebook.com", "t.co", "vk.com"}
	}

	tMin := (time.Now().UTC().UnixNano() / 1e6) - 864e5

	q := "SELECT ts, hostname, total, unique FROM host_counts WHERE hostname IN ? AND ts > ?"
	iter := session.Query(q, hostnames, tMin).Iter()

	for iter.Scan(&count.Timestamp, &count.Hostname, &count.Total, &count.Unique) {
		counts = append(counts, count)
	}

	jsonHandler(w, r, counts)
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
