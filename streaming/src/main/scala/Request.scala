package com.github.abtrout._1USAgov_club

import argonaut._, Argonaut._
import java.net.URI

case class Request(
  ts: Long,
  bitlyHash: String,
  createdAt: Long,
  sourceURL: String,
  destURL: String,
  knownUser: Int,
  userAgent: String,
  countryCode: String)

object Request {

  // 1.USA.gov data comes in as JSON, but with wacky keys. For details,
  // see <https://github.com/usagov/1.USA.gov-Data#how-to-access-the-data>
  implicit def requestCodec: CodecJson[Request] = {
    casecodec8(Request.apply, Request.unapply)("t", "g", "hc", "r", "u", "nk", "a", "c")
  }
}
