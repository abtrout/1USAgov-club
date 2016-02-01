DROP KEYSPACE IF EXISTS oneusa ;

CREATE KEYSPACE oneusa
  WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 3
  };

CREATE TABLE oneusa.host_counts (
  day timestamp,
  hour timestamp,
  minute timestamp,
  hostname text,
  total counter,
  unique counter,
  PRIMARY KEY (day, hour, minute, hostname)
) with CLUSTERING ORDER BY (hour DESC, minute DESC, hostname ASC);

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
  agents int,
  PRIMARY KEY (day, ts)
) with CLUSTERING ORDER BY (ts DESC);
