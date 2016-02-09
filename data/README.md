## About the data

Since 2011, Bitly has partnered with 1.USA.gov to shorten government URLs to 1.USA.gov URLs, and USA.gov provides JSON data for all the requests it handles.

Measured Voice maintains an [archive](http://1usagov.measuredvoice.com/) of this data from mid-2011 to mid-2013. The provided shell script will download (and validate) it all. After discarding lines with invalid JSON, we're left with about 20GB of data -- just under 50MM requests.

```
$ wc -l *.json
 10477461 1.USA.gov-2011-raw.json
 25031506 1.USA.gov-2012-raw.json
 14481978 1.USA.gov-2013-raw.json
 49990945 total
$ du -ch *.json
4.2G    1.USA.gov-2011-raw.json
9.6G    1.USA.gov-2012-raw.json
5.7G    1.USA.gov-2013-raw.json
20G     total
```

Since 1USAgov club's focus is on real-time processing, this data is repeatedly [replayed](https://github.com/abtrout/1USAgov-club/blob/master/processing/src/main/scala/Request.scala#L27-L28) to produce desirable rates of traffic.
