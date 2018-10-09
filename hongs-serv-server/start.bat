@echo off

set CORE_PATH=%~DP0

"%CORE_PATH%bin\hco.cmd" server.start --DEBUG 1 %*

@echo on