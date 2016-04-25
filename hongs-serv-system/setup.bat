@echo off

set CORE_PATH=%~DP0\

"%CORE_PATH%\bin\app.cmd" system:setup $@

@echo on
