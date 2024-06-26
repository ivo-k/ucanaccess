@ECHO OFF
SET PATH=%PATH%;.
SET BASE_DIR=%~f0

:CONT
SET RMVD=%BASE_DIR:~-1%
SET BASE_DIR=%BASE_DIR:~0,-1%
if NOT "%RMVD%"=="\" goto CONT

SET UCANACCESS_HOME=%BASE_DIR%
SET LOCAL_HOME_JAVA="%JAVA_HOME%"
IF EXIST %LOCAL_HOME_JAVA%\bin\java.exe (
    SET LOCAL_JAVA=%LOCAL_HOME_JAVA%\bin\java.exe
) ELSE (
    SET LOCAL_JAVA=java.exe
)

%LOCAL_JAVA% -version
@ECHO.

SET CLASSPATH="%UCANACCESS_HOME%\${artifact.artifactId}-${artifact.version}-${uber.jar.classifier.name}.jar"

%LOCAL_JAVA% -jar %CLASSPATH%
PAUSE
