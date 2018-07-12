@echo off

set CORE_PATH=%~DP0\

"%CORE_PATH%\bin\hco.cmd" system.setup $@

@echo on
