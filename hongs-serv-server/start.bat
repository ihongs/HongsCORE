@echo off

set CORE_PATH=%~DP0\..

"%CORE_PATH%\bin\foo.cmd" server.start $@

@echo on
