@echo off

SETLOCAL

set CURR_PATH=%~DP0
set CORE_PATH=%CURR_PATH%..
set JAVA_PATH=%JAVA_HOME%\bin\java
set KLASSPATH=%CLASSPATH%;%CORE_PATH%\lib\*

"%JAVA_PATH%" %JAVA_OPTS% ^
  -classpath "%KLASSPATH%"^
  -Dlogs.dir="\\%CORE_PATH%\var\log"^
  -Dtmps.dir="\\%CORE_PATH%\var\tmp"^
  io.github.ihongs.combat.CombatRunner %*^
  --COREPATH "%CORE_PATH%"

ENDLOCAL

@echo on
