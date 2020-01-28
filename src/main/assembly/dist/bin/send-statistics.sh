#!/usr/bin/env bash

# input parameters log-files source directory and log-files target directory
YESTERDAY=`date --date=yesterday +%Y-%m-%d`
LOG_FILE="$1/easy-statistics.$YESTERDAY.log"
LOG_FILE_TARGET="$2/statistics.log.${YESTERDAY}_dls"

if [ -f "$LOG_FILE" ]; then
  LINE_COUNT=$(wc -l < "$LOG_FILE")
  if [[ $LINE_COUNT -gt 0 ]]; then
    echo "Sending yesterday's statistics logging..."
    scp $LOG_FILE $LOG_FILE_TARGET
    echo "Sent yesterday's statistics logging."
  else
    echo "File $LOG_FILE exists, but is empty; nothing to send"
  fi
else
  echo "File $LOG_FILE does not exist; nothing to send"
fi

exit $?
