@echo off

set CORE_PATH=%~DP0\..

"%CORE_PATH%\bin\app.cmd" server:start $@

@echo on
