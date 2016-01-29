#!/usr/bin/env bash

#CASSANDRA_HOSTS=""
#CASSANDRA_KEYSPACE=""

[ ! -f clubd ] && go build
./clubd -hosts $CASSANDRA_HOSTS -keyspace $CASSANDRA_KEYSPACE
