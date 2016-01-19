#!/usr/bin/env bash

#KAFKA_PEERS=""
#KAFKA_TOPIC=""

# If `stream_producer` hasn't been build, then we build it.
[ ! -f stream_producer ] && go build

./stream_producer -brokers $KAFKA_PEERS -topic $KAFKA_TOPIC
