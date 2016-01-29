package main

import (
	"compress/gzip"
	"encoding/json"
	"log"
	"net/http"
	"net/url"
	"strings"
)

func jsonHandler(w http.ResponseWriter, r *http.Request, d interface{}) {
	var response []byte
	var err error

	// Output will be pretty printed for requests including ?pretty.
	if prettyPrint(r) {
		response, err = json.MarshalIndent(&d, "", " ")
	} else {
		response, err = json.Marshal(&d)
	}

	if err != nil {
		log.Println("Failed to handle JSON: ", err, d)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// The response will be compressed if requested with Accept-Encoding header.
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	if strings.Contains(r.Header.Get("Accept-Encoding"), "gzip") {
		gz := gzip.NewWriter(w)
		defer gz.Close()

		w.Header().Set("Content-Encoding", "gzip")
		gz.Write(response)
	} else {
		w.WriteHeader(http.StatusOK)
		w.Write(response)
	}
}

func prettyPrint(r *http.Request) bool {
	ps, _ := url.ParseQuery(r.URL.RawQuery)
	_, isPretty := ps["pretty"]

	return isPretty
}
