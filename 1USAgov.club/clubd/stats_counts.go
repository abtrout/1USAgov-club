package main

import (
	"github.com/julienschmidt/httprouter"
	"net/http"
	"net/url"
	"strconv"
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
	hostnames, n := parseHostCountParams(r.URL.RawQuery)

	// We're looking for records between tMin and tMax
	tMax := time.Now().UTC().UnixNano() / 1e6
	tMin := tMax - int64(60000*n)

	// It's possible we'll be querying over a range of days (our primary key), so we
	// compute these ahead of time for use with `WHERE IN`
	dayMax := tMax - (tMax % 864e5)
	dayMin := tMin - (tMin % 864e5)
	days := []int64{dayMax, dayMin}

	cql := "SELECT minute, hostname, total, unique FROM host_counts WHERE day IN ? AND hostname IN ? AND minute > ?"
	iter := session.Query(cql, days, hostnames, tMin).Iter()

	counts := []HostCount{}
	var count HostCount

	for iter.Scan(&count.Timestamp, &count.Hostname, &count.Total, &count.Unique) {
		counts = append(counts, count)
	}

	jsonHandler(w, r, counts)
}

func parseHostCountParams(raw string) ([]string, int) {
	var hostnames []string
	defaultHostnames := []string{"nsa.gov", "jpl.nasa.gov", "whitehouse.gov"}

	var n int
	defaultN := 30

	ps, err := url.ParseQuery(raw)
	if err != nil {
		return defaultHostnames, defaultN
	}

	ns, ok := ps["n"]
	if !ok {
		n = defaultN
	} else {
		n, err = strconv.Atoi(ns[0])
		if err != nil {
			n = defaultN
		}
	}

	hs, ok := ps["hostnames"]
	if !ok {
		hostnames = defaultHostnames
	} else {
		hostnames = strings.Split(hs[0], ",")
	}

	return hostnames, n
}
