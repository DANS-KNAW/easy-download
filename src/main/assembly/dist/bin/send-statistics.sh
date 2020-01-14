#!/usr/bin/env bash

# input parameters log-files source directory and log-files target directory
YESTERDAY=`date --date=yesterday +%Y-%m-%d`
LOG_FILE="$1/easy-statistics.$YESTERDAY.log"
LOG_FILE_TARGET="$2/statistics.log.${YESTERDAY}_dls"

echo "Sending yesterday's statistics logging ... "
scp $LOG_FILE $LOG_FILE_TARGET
exit $?
