@echo off

SETLOCAL

set CURR_PATH=%~DP0
set CORE_PATH=%CURR_PATH%..
set JAVA_PATH=%JAVA_HOME%\bin\java
set KLASSPATH=%CLASSPATH%;%CORE_PATH%\lib\*;%CORE_PATH%\classes;%CORE_PATH%\lib\classes

"%JAVA_PATH%" %JAVA_OPTS% ^
  -classpath "%KLASSPATH%"^
  -Dlog4j.configurationFile="\\%CORE_PATH%\etc\log.xml"^
  -Dlogs.dir="\\%CORE_PATH%\var\log"^
  -Dtmps.dir="\\%CORE_PATH%\var\tmp"^
  io.github.ihongs.combat.CombatRunner %*^
  --COREPATH "%CORE_PATH%"

ENDLOCAL

@echo on