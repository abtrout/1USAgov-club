#!/usr/bin/env bash

# Currently, Measured Voice provides 1.USA.gov data archives for
# requests made between mid-2011 to mid-2013. This script
# downloads all of them.
#
# See http://1usagov.measuredvoice.com/ for more details.
function get_archives {
  echo "Downloading 1USA.gov archives for $YEAR:"
  BASE_URL="http://1usagov.measuredvoice.com"
  TMP_FILE=$(mktemp)

  get_filenames
  get_files
  
  rm -f $TMP_FILE
  echo " > DONE"
}

# We scrape the list of filenames from their website.
function get_filenames {
  echo -ne " > Getting filenames..."

  LINK_TAG="<a href=\"/bitly_archive/usagov_bitly_data[0-9-]+.gz\">"
  curl -s "$BASE_URL/$YEAR/" | egrep -o "$LINK_TAG" | cut -d\" -f2 >> $TMP_FILE
  echo
}

# Once we have the full list, we download them; uncompressing and validating
# the JSON with jq (https://stedolan.github.io/jq/) as we go.
#
# Since there are LOTS of files, we keep track of progress for sanity!
function get_files {
  OUTFILE="1.USA.gov-$YEAR-raw.json"
  rm -f $OUTFILE

  TOTAL=($(wc -l $TMP_FILE))
  CURRENT=0

  while read LINE; do
    CURRENT=$((CURRENT + 1))
    echo -ne " > Downloading files ($CURRENT/$TOTAL)\r"

    FILE=${LINE##*/}; FILE=${FILE%."gz"}
    curl -s "$BASE_URL$LINE" | gunzip -c | jq -c . >> $OUTFILE
  done < $TMP_FILE

  echo
}

# Archived data is available for 2011, 2012, and 2013.
for YEAR in {2011,2012,2013}; do get_archives $YEAR; done
