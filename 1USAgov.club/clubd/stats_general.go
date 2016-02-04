package main

import (
	"github.com/gocql/gocql"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"time"
)

type GenStats struct {
	Timestamp         int64 `json:"ts"`
	RequestsPerSecond int64 `json:"requestsPerSecond"`
	GovHosts          int   `json:"governmentHosts"`
	CountryCodes      int   `json:"countryCodes"`
}

func GeneralStats(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	var stats GenStats

	q := "SELECT ts, reqps, govurls, countries FROM gen_stats WHERE DAY = ? ORDER BY ts DESC LIMIT 1"

	ts := time.Now().UTC().UnixNano() / 1e6
	day := ts - (ts % 864e5)

	err := session.Query(q, day).Scan(&stats.Timestamp,
		&stats.RequestsPerSecond, &stats.GovHosts, &stats.CountryCodes)

	if err == gocql.ErrNotFound {
		w.WriteHeader(http.StatusNotFound)
		return
	} else if err != nil {
		log.Println("Failed to get gen_stats: ", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	jsonHandler(w, r, stats)
}
