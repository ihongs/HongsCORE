@echo off

set CORE_PATH=%~DP0

"%CORE_PATH%bin\hdo.cmd" server.stop < nul
