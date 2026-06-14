@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.  See the NOTICE file distributed with
@REM this work for additional information regarding copyright ownership.
@REM The ASF licenses this file to You under the Apache License, Version 2.0
@REM (the "License"); you may not use this file except in compliance with
@REM the License.  You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@REM Set default: run at normal priority
@IF NOT "%MAVEN_BATCH_PRIORITY%"=="" SET "MAVEN_BATCH_PRIORITY=%MAVEN_BATCH_PRIORITY%"

@REM set %HOME% to equivalent of $HOME
IF "%HOME%"=="" (SET "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Execute a user defined script before this one
IF NOT "%MAVEN_SKIP_RC%"=="" GOTO skipRc
IF EXIST "%USERPROFILE%\mavenrc_pre.cmd" CALL "%USERPROFILE%\mavenrc_pre.cmd"
:skipRc

@setlocal

SET ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
IF NOT "%JAVA_HOME%"=="" GOTO OkJHome

FOR %%j IN (java.exe) DO SET "JAVA_EXE=%%~$PATH:j"
IF NOT "%JAVA_EXE%"=="" GOTO OkJHome

ECHO.
ECHO Error: JAVA_HOME not found in your environment. >&2
ECHO Please set the JAVA_HOME variable in your environment to match the >&2
ECHO location of your Java installation. >&2
ECHO.
GOTO error

:OkJHome
IF EXIST "%JAVA_HOME%\bin\java.exe" GOTO init

ECHO.
ECHO Error: JAVA_HOME is set to an invalid directory. >&2
ECHO JAVA_HOME = "%JAVA_HOME%" >&2
ECHO Please set the JAVA_HOME variable in your environment to match the >&2
ECHO location of your Java installation. >&2
ECHO.
GOTO error
@REM ==== END VALIDATION ====

:init
SET "MAVEN_CMD_LINE_ARGS=%MAVEN_CONFIG% %*"

@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current directory if not found.

SET "MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%"
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

SET "EXEC_DIR=%CD%"
SET "WDIR=%EXEC_DIR%"
:findBaseDir
IF EXIST "%WDIR%\.mvn" GOTO baseDirFound
SET "WDIR=%WDIR%\.."
IF "%WDIR%"=="%WDIR%\.." GOTO baseDirNotFound
GOTO findBaseDir

:baseDirFound
SET "MAVEN_PROJECTBASEDIR=%WDIR%"
GOTO endDetectBaseDir

:baseDirNotFound
SET "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
GOTO endDetectBaseDir

:endDetectBaseDir
IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" GOTO endReadJvmConfig

@setlocal EnableExtensions EnableDelayedExpansion
FOR /F "usebackq delims=" %%a IN ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") DO SET JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
@endlocal & SET JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

:endReadJvmConfig

@REM Determine the download URL for the Maven wrapper jar
SET "DOWNLOAD_URL="
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

FOR /F "usebackq tokens=1,2 delims==" %%a IN ("%WRAPPER_PROPERTIES%") DO (
    IF "%%a"=="wrapperUrl" SET "DOWNLOAD_URL=%%b"
)

@REM Find the mvn command
IF "%MVNW_REPOURL%"=="" (
    SET "MVNW_REPOURL=https://repo.maven.apache.org/maven2"
)

@REM Download the maven-wrapper.jar if missing
IF EXIST "%WRAPPER_JAR%" GOTO runMaven

SET "JAR_URL=%DOWNLOAD_URL%"

IF "%MVNW_VERBOSE%"=="true" (
    ECHO Downloading maven wrapper from %JAR_URL%
)

IF NOT "%MVNW_USERNAME%"=="" (
    SET "MVNW_AUTH=-u %MVNW_USERNAME%:%MVNW_PASSWORD%"
)

SET "MVNW_INTERNAL_SKIP_SELF_UPDATE=true"

@REM Try PowerShell to download
powershell -Command "&{"^
    "$webclient = new-object System.Net.WebClient;"^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%'))) {"^
    "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
    "}"^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;"^
    "$webclient.DownloadFile('%JAR_URL%', '%WRAPPER_JAR%')"^
    "}" >NUL 2>&1
IF "%ERRORLEVEL%"=="0" GOTO runMaven

ECHO Failed to download %JAR_URL% >&2
EXIT /B 1

:runMaven
SET "MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java.exe"
SET "JVM_CONFIG_MAVEN_PROPS="

FOR /F "usebackq delims=" %%a IN ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") DO SET "JVM_CONFIG_MAVEN_PROPS=%%a"

@REM Run Maven using the wrapper
"%MAVEN_JAVA_EXE%" ^
    %JVM_CONFIG_MAVEN_PROPS% ^
    %MAVEN_OPTS% ^
    %MAVEN_DEBUG_OPTS% ^
    -classpath "%WRAPPER_JAR%" ^
    "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
    org.apache.maven.wrapper.MavenWrapperMain ^
    %MAVEN_CMD_LINE_ARGS%

IF ERRORLEVEL 1 GOTO error
GOTO end

:error
SET ERROR_CODE=1

:end
@endlocal & SET ERROR_CODE=%ERROR_CODE%

IF NOT "%MAVEN_SKIP_RC%"=="" GOTO skipRcPost
IF EXIST "%USERPROFILE%\mavenrc_post.cmd" CALL "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

IF "%MAVEN_BATCH_PAUSE%"=="on" PAUSE

IF "%MAVEN_EXITCODE%"=="auto" SET ERRORLEVEL=%ERROR_CODE%

EXIT /B %ERROR_CODE%
