@echo off
setlocal
cd /d %~dp0
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 goto :eof
java -cp out StudentGradeTrackerGUI
