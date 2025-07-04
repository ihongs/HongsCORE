@echo off

SETLOCAL

set CURR_PATH=%~DP0
set CORE_PATH=%CURR_PATH%..
set JAVA_PATH=%JAVA_HOME%\bin\java
set KLASSPATH=%CLASSPATH%;%CORE_PATH%\classes;%CORE_PATH%\lib\classes;%CORE_PATH%\lib\*

set LINES=
set COLUMNS=
for /F "skip=4 tokens=2 delims=: " %%A in (
  'mode con'
) do (
  if not defined LINES (
    set LINES=%%A
  ) else (
    set COLUMNS=%%A
    goto QUIT
  )
)
:QUIT
@rem echo %LINES% lines %COLS% columns

"%JAVA_PATH%" %JAVA_OPTS% ^
  -classpath "%KLASSPATH%"^
  -Dfile.encoding="UTF-8" ^
  -Dlog4j.configurationFile="\\%CORE_PATH%\etc\log.xml"^
  -Dlogs.dir="\\%CORE_PATH%\var\log"^
  -Dtmps.dir="\\%CORE_PATH%\var\tmp"^
  io.github.ihongs.combat.CombatRunner %*^
  --COREPATH "%CORE_PATH%"

ENDLOCAL

@echo on