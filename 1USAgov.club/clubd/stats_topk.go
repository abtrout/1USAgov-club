package main

import (
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"time"
)

type TopHosts struct {
	Timestamp int64          `json:"ts"`
	Inbound   map[string]int `json:"inbound"`
	Outbound  map[string]int `json:"outbound"`
}

func TopLeaders(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	tMax := time.Now().UTC().UnixNano() / 1e6
	day := tMax - (tMax % 864e8)
	cql := "SELECT ts, topkin, topkout FROM topk WHERE day = ? AND ts < ? LIMIT 1"

	var topk TopHosts
	err := session.Query(cql, day, tMax).Scan(&topk.Timestamp, &topk.Inbound, &topk.Outbound)

	if err != nil {
		log.Println("Failed to get TopK: ", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	jsonHandler(w, r, topk)
}
