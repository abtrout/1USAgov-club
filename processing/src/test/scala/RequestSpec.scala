package com.github.abtrout._1USAgov_club

import argonaut._, Argonaut._
import org.specs2.mutable.Specification

class RequestSpec extends Specification with RequestHelpers {

  // We rely on parsing incoming JSON to a Request for processing. I ran into
  // some issues which turned out to be caused by this, so I've added this unit
  // test to catch future problems.
  "JSON decoder should succeed on valid input" in {
    val json = List(
      """{"h":"XdUNr","g":"XdUNr","l":"bitly","hh":"j.mp","u":"http://www.usno.navy.mil/NOOC/nmfc-ph/RSS/jtwc/ab/abpwsair.jpg","r":"http://forum9.hkgolden.com/view.aspx?type=ST\u0026message=6223008","a":"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36","i":"","t":1453524642,"k":"","nk":0,"hc":1254116965,"_id":"52795867-a1e2-6062-f5a4-22d9e1ce4ca9","al":"zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4","c":"HK","tz":"Asia/Hong_Kong","gr":"00","cy":"Central District","ll":[22.2833,114.15]}""",
      """{"h":"1YVJiNX","g":"WoDdPr","l":"anonymous","hh":"bit.ly","u":"http://www.ncbi.nlm.nih.gov/pubmed/23049819","r":"http://m.facebook.com/","a":"Mozilla/5.0 (Linux; Android 5.0.1; SAMSUNG-SGH-I537 Build/LRX22C; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/59.0.0.15.313;]","i":"","t":1453524642,"k":"","nk":0,"hc":1451669612,"_id":"cbf41853-c64f-478d-a4f1-12e3d60b77c6","al":"en-US","c":"US","tz":"America/Chicago","gr":"TX","cy":"San Antonio","mc":641,"ll":[29.6569,-98.5107]}""",
      """{"h":"1nDash1","g":"1nDash2","l":"anonymous","hh":"1.usa.gov","u":"http://www.af.mil/News/ArticleDisplay/tabid/223/Article/643990/f-35-fires-first-aim-9x-missile.aspx","r":"http://m.facebook.com","a":"Mozilla/5.0 (iPhone; CPU iPhone OS 9_2_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13D15 [FBAN/FBIOS;FBAV/43.0.0.26.314;FBBV/17000587;FBDV/iPhone6,1;FBMD/iPhone;FBSN/iPhone OS;FBSV/9.2.1;FBSS/2; FBCR/AT\u0026T;FBID/phone;FBLC/en_US;FBOP/5]","i":"","t":1453524658,"k":"","nk":0,"hc":1453469457,"_id":"acdb2120-f7ec-d3f8-34dc-8ceb6a4ba875","al":"en-us","c":"US","tz":"America/Chicago","gr":"TX","cy":"Arlington","mc":623,"ll":[32.6916,-97.0888]}""",
      """{"h":"1VdOrA6","g":"1n8j7Yh","l":"anonymous","hh":"1.usa.gov","u":"http://www.weather.gov/media/phi/current_briefing.pdf","r":"https://t.co/ccy74sG7yS","a":"Mozilla/5.0 (Linux; Android 5.0; SM-G900V Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36","i":"","t":1453524663,"k":"","nk":0,"hc":1453476192,"_id":"b9ee562b-a237-ca4d-4022-c3d3251130f6","al":"en-US,en;q=0.8","c":"US","tz":"America/New_York","gr":"PA","cy":"Glen Mills","mc":504,"ll":[39.9056,-75.4874]}""",
      """{"h":"1lJgufr","g":"1lJgufs","l":"nmusafmup","hh":"1.usa.gov","u":"http://www.nationalmuseum.af.mil/Visit/MuseumExhibits/FactSheets/Display/tabid/509/Article/197498/unbroken-will-the-lance-sijan-story.aspx","r":"http://m.facebook.com","a":"Mozilla/5.0 (iPad; CPU OS 9_0_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13A452 [FBAN/FBIOS;FBAV/46.0.0.54.156;FBBV/18972819;FBDV/iPad2,3;FBMD/iPad;FBSN/iPhone OS;FBSV/9.0.2;FBSS/1; FBCR/Verizon;FBID/tablet;FBLC/en_US;FBOP/1]","i":"","t":1453524664,"k":"","nk":0,"hc":1450124423,"_id":"c3ad81bb-6fae-3593-7a4f-50005ba9579f","al":"en-us","c":"US","tz":"America/New_York","gr":"SC","cy":"Irmo","mc":546,"ll":[34.1422,-81.2047]}""")

    val success: Boolean = json.foldLeft(true) { (acc, line) =>
      acc && line.decodeOption[Request].isDefined
    }

    success mustEqual true
  }

  // 1USAgov includes the referring URL (if available) which we use as our
  // our Request.sourceURL. DIRECT visits are a special case, and some URLS 
  // inevitably fail to be parsed, in which case we should expect INVALID. 
  "Extract hostnames from provided URLs" in {
    parseHost("direct") mustEqual "DIRECT"
    parseHost("someHost.com///?") mustEqual "INVALID"
    parseHost("https://google.com/chrome") mustEqual "google.com"
    parseHost("http://www.google.com/chrome") mustEqual "google.com"
    parseHost("http://ww.google.com/chrome") mustEqual "ww.google.com"
  }

  "Split timestamps into day/hour/minute/second" in {
    val (day, hour, minute, second) = splitTimestamp(1454210082106L)

    second mustEqual 1454210082000L
    minute mustEqual 1454210040000L
    hour mustEqual 1454209200000L
    day mustEqual 1454198400000L
  }
}
