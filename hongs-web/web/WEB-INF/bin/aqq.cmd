@echo off

set JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787"
set CURR_PATH=%~DP0
%CURR_PATH%\app.cmd %*

@echo on
