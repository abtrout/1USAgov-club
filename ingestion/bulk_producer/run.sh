#!/usr/bin/env bash

#KAFKA_PEERS=""
#KAFKA_TOPIC=""
#INPUT_FILE=""

# If `bulk_producer` hasn't been build, then we build it.
[ ! -f bulk_producer ] && go build

./bulk_producer -brokers $KAFKA_PEERS -topic $KAFKA_TOPIC -file $INPUT_FILE
