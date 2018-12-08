@echo off

@REM  Copyright (C) 2016 LibRec

@REM  This file is part of LibRec.
@REM  LibRec is free software: you can redistribute it and/or modify
@REM  it under the terms of the GNU General Public License as published by
@REM  the Free Software Foundation, either version 3 of the License, or
@REM  (at your option) any later version.

@REM  LibRec is distributed in the hope that it will be useful,
@REM  but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
@REM  GNU General Public License for more details.

@REM  You should have received a copy of the GNU General Public License
@REM  along with LibRec. If not, see <http://www.gnu.org/licenses/>.

setlocal enabledelayedexpansion

rem CONSTRUCT CLASSPATH
set PWD=%~dp0
set LIBREC_HOME=%PWD:~0,-5%
set LIB=%LIBREC_HOME%\lib
set LIB_CLASSPATH=
for /f "delims=" %%f in ('dir /b /od "%LIB%"\*.jar') do (set "LIB_CLASSPATH=!LIB_CLASSPATH!%LIB%\%%f;")
set CLASSPATH=%LIB_CLASSPATH%%LIBREC_HOME%\conf;%LIBREC_HOME%\bin;
set JAVA_ARG=
set LIBREC_MAIN=net.librec.tool.driver.DataDriver
set IS_VERBOSE="false"

rem SHOW HELP OR VERSION
if "%~1" equ "" goto ECHO_HELP
if "%~1" equ "help" goto ECHO_HELP
if "%~1" equ "-h" goto ECHO_HELP
if "%~1" equ "-help" goto ECHO_HELP
if "%~1" equ "--help" goto ECHO_HELP
if "%~1" equ "version" goto ECHO_VERSION
if "%~1" equ "-v" goto ECHO_VERSION
if "%~1" equ "-version" goto ECHO_VERSION
if "%~1" equ "--version" goto ECHO_VERSION
rem GET LIBREC_MAIN
if "%~1" equ "data" (set LIBREC_MAIN=net.librec.tool.driver.DataDriver)
if "%~1" equ "rec" (set LIBREC_MAIN=net.librec.tool.driver.RecDriver)

shift

rem GET JAVA_ARG
:LOOP
if "%~1" equ "" goto END

	
	if "%~1" neq "-exec" goto NOT_EXEC
		set "JAVA_ARG=!JAVA_ARG!-exec"
		shift
		goto LOOP

	:NOT_EXEC
	if "%~1" neq "-build" goto NOT_BUILD
		set "JAVA_ARG=!JAVA_ARG!-build"
		shift
		goto LOOP

	:NOT_BUILD
	if "%~1" neq "-load" goto NOT_LOAD
		set "JAVA_ARG=!JAVA_ARG!-load"
		shift
		goto LOOP

	:NOT_LOAD
	if "%~1" neq "-save" goto NOT_SAVE
		set "JAVA_ARG=!JAVA_ARG!-save"
		shift
		goto LOOP

	:NOT_SAVE
	if "%~1" neq "-D" goto NOT_D
		set "JAVA_ARG=!JAVA_ARG! -D"
		shift
		set "JAVA_ARG=!JAVA_ARG! %~1"
		shift
		set "JAVA_ARG=!JAVA_ARG!=%~1"
		shift
		goto LOOP

	:NOT_D
	if "%~1" neq "-jobconf" goto NOT_JOBCONF
		set "JAVA_ARG=!JAVA_ARG! -jobconf"
		shift
		set "JAVA_ARG=!JAVA_ARG! %~1"
		shift
		set "JAVA_ARG=!JAVA_ARG!=%~1"
		shift
		goto LOOP

	:NOT_JOBCONF
	if "%~1" neq "-conf" goto NOT_CONF
		set "JAVA_ARG=!JAVA_ARG! -conf"
		shift
		set "JAVA_ARG=!JAVA_ARG! %~1"
		shift
		goto LOOP

	:NOT_CONF
	if "%~1" neq "-libjars" goto NOT_LIBJARS
		shift
		set CLASSPATH=%LIB_CLASSPATH%%~1
		shift
		:GETJARS
		set temp=%~1
		set jarFile=%temp:~-4%
		if "%jarFile%" equ ".jar" (
			set CLASSPATH=%CLASSPATH%;%~1
			shift
			goto :GETJARS
		)

		goto LOOP
		:NOT_LIBJARS
		if "%~1" neq "-verbose" goto NOT_VERBOSE
		shift
		set IS_VERBOSE="true"

		goto LOOP

	:NOT_VERBOSE
:END

rem GET AND CHECK JAVA_VERSION
PATH %PATH%;%JAVA_HOME%\bin\
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j%%k%%l%%m"
if %jver% LSS 17000 (
	echo. Please update your JDK version to 1.7 or higher
) else (
	if %IS_VERBOSE% equ "true" (
		rem echo. JDK version: %jver%
		echo. CLASSPATH:%CLASSPATH%
		echo. LIBREC_MAIN:%LIBREC_MAIN%
		echo. JAVA_ARG:%JAVA_ARG%
	)

	java -cp "%CLASSPATH%" %LIBREC_MAIN% %JAVA_ARG%
)

goto :skip
:ECHO_HELP
	echo.
	echo Usage: librec [command] [options]...
	echo.
	echo commands:
    echo rec                       run recommender
    echo data                      load data
    echo.
    echo global options:
    echo -help                    display this help text
    echo -exec                    run Recommender
    echo -version                 show Librec version info
    echo.
    echo job options:
    echo -conf [file]              path to config file
    echo -D, -jobconf [prop]       set configuration items (key=value)
    echo -libjars                  add entend jar files to classpath
    echo -verbose                  print details of parameters

goto :skip

:ECHO_VERSION
	echo.
	echo LibRec 2.0.0
	echo.

:skip