@echo off

set HTTP_PORT=8080
set CORE_PATH=%~DP0

echo Press Ctrl+C to stop the server, or run stop.bat

"%CORE_PATH%bin\hdo.cmd" server start %HTTP_PORT% --DEBUG 6 < nul
