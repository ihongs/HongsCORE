@echo off

set CORE_PATH=%~DP0

"%CORE_PATH%bin\hdo.cmd" source.setup --DEBUG 0 %*

@echo on