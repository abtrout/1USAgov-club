DROP KEYSPACE IF EXISTS oneusa ;

CREATE KEYSPACE oneusa
  WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 3
  };

// Early plans included combining daily/hourly/minutely counts into the same
// table and aggregating over the results to get final counts. Unfortunately,
// COUNTERs can't be aggregated (using built-ins, at least), so we're forced
// to keep these queries in separate tables (#nosqlProblems).

CREATE TABLE oneusa.counts_minute (
  day timestamp,
  minute timestamp,
  hostname text,
  total counter,
  unique counter,
  PRIMARY KEY (day, hostname, minute)
) with CLUSTERING ORDER BY (hostname ASC, minute DESC);

CREATE TABLE oneusa.counts_hour (
  day timestamp,
  hour timestamp,
  hostname text,
  total counter,
  unique counter,
  PRIMARY KEY (day, hostname, hour)
) with CLUSTERING ORDER BY (hostname ASC, hour DESC);

CREATE TABLE oneusa.counts_day (
  day timestamp,
  hostname text,
  total counter,
  unique counter,
  PRIMARY KEY (day, hostname)
) with CLUSTERING ORDER BY (hostname ASC);

CREATE TABLE oneusa.topk (
  day timestamp,
  ts timestamp,
  topkin map<text, int>,
  topkout map<text, int>,
  PRIMARY KEY (day, ts)
) with CLUSTERING ORDER BY (ts DESC);

CREATE TABLE oneusa.gen_stats (
  day timestamp,
  ts timestamp,
  reqps int,
  govurls int,
  countries int,
  PRIMARY KEY (day, ts)
) with CLUSTERING ORDER BY (ts DESC);
