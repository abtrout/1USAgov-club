package main

import (
	"github.com/gocql/gocql"
	"github.com/julienschmidt/httprouter"
	"net/http"
	"time"
)

type TopHosts struct {
	Timestamp int64          `json:"ts"`
	Inbound   map[string]int `json:"inbound"`
	Outbound  map[string]int `json:"outbound"`
}

func TopLeaders(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	var topk TopHosts

	tMax := time.Now().UTC().UnixNano() / 1e6
	day := tMax - (tMax % 864e5)
	cql := "SELECT ts, topkin, topkout FROM topk WHERE day = ? AND ts < ? LIMIT 1"

	err := session.Query(cql, day, tMax).Scan(&topk.Timestamp, &topk.Inbound, &topk.Outbound)
	if err == gocql.ErrNotFound {
		w.WriteHeader(http.StatusNotFound)
		return
	} else if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	jsonHandler(w, r, topk)
}
