package com.github.abtrout._1USAgov_club

import argonaut._, Argonaut._
import scala.util.{Try, Success}
import java.net.URI

case class Request(
  ts: Long,
  sourceURL: String,
  destURL: String,
  knownUser: Int,
  countryCode: String)

object Request extends RequestHelpers {

  // 1.USA.gov data comes in as JSON, but with wacky keys. For details,
  // see <https://github.com/usagov/1.USA.gov-Data#how-to-access-the-data>
  implicit def requestDecoder: DecodeJson[Request] =
    DecodeJson(cur => for {
      t <- (cur --\ "t").as[Long]
      r <- (cur --\ "r").as[String]
      u <- (cur --\ "u").as[String]
      nk <- (cur --\ "nk").as[Int]
      c <- (cur --\ "c").as[String]
      // 1USAgov sends us timestamps in seconds; we want milliseconds!
      // ts = t * 1000
      // MANUAL OVERRIDE for replaying old data:
      ts = System.currentTimeMillis
    } yield Request(ts, parseHost(r), parseHost(u), nk, c))
}

trait RequestHelpers {

  // All of our incoming data comes with a timestamp, but we will usually want to
  // round down to day/hour/minute for more convenient interfacing with Cassandra.
  def splitTimestamp(ts: Long) = {
    (roundDay(ts), roundHour(ts), roundMinute(ts), roundSecond(ts))
  }

  def roundTimestamp(ts: Long, ms: Int = 1): Long = ts - (ts % ms)
  def roundSecond(ts: Long, n: Int = 1) = roundTimestamp(ts, 1000 * n)
  def roundMinute(ts: Long, n: Int = 1) = roundSecond(ts, 60 * n)
  def roundHour(ts: Long, n: Int = 1) = roundMinute(ts, 60 * n)
  def roundDay(ts: Long) = roundHour(ts, 24)

  // Incoming data includes two URLs: the referring URL, the destination URL.
  // For most of our stats, we're only interested in hostnames though.
  //
  // Some requests are made directly, so we handle that case separately.
  def parseHost(url: String): String = {
    if(url == "direct") return "DIRECT"

    Try(new URI(url.split(" ")(0)).getHost) match {
      case Success(host) if host != null =>
        host.replaceFirst("^www\\.", "")
      case _ => "INVALID"
    }
  }
}
