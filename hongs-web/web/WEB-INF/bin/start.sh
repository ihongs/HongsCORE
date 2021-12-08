#!/bin/bash

DEBUG=2
SPORT=8080
CWD="$(dirname "$0")"
CWD="$(cd "$PWD"/..; pwd)"
PID="$CWD/var/server/$SPORT.pid"
LOG="$CWD/var/log/run.out"
APP="$CWD/bin/hdo"
USR="nginx"

# Make sure the process is stoped, and pid file is removed
if [ -f "$PID" ]
then
  PID=`cat "$PID"`
  kill $PID
  # Wati 3 seconds
  if ps -p $PID > /dev/null; then sleep 1
  if ps -p $PID > /dev/null; then sleep 1
  if ps -p $PID > /dev/null; then sleep 1
  if ps -p $PID > /dev/null; then echo 'Start timeout'; exit
  fi; fi; fi; fi
fi

# start for docker
exec "$APP" server.start $SPORT --DEBUG $DEBUG

# run in background
#nohup sh "$APP" server.start $SPORT --DEBUG $DEBUG > "$LOG" 2>&1 &

# run in background with user, must export JAVA_HOME
#su -m -s /bin/bash -c "nohup sh '$APP' server.start $SPORT --DEBUG $DEBUG > '$LOG' 2>&1 &" $USR
