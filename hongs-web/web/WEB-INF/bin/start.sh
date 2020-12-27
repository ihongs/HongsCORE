#!/bin/bash

# Just for docker starts.

DEBUG=2
SPORT=8080
PWD=$(cd `dirname $0`; pwd)
CWD=`dirname $PWD`
APP="$CWD/bin/hdo"
PID="$CWD/var/server/$SPORT.pid"

# Make sure the process is stoped, and pid file is removed
if [ -f "$PID" ]
then
  kill `cat "$PID"`
  rm -f "$PID"
  sleep 3
fi

exec "$APP" server.start $SPORT --DEBUG $DEBUG
