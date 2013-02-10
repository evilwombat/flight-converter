@echo off

if exist "C:\Program Files (x86)\Java\jre7\bin\java.exe" (
	"C:\Program Files (x86)\Java\jre7\bin\java.exe" -jar "%~p0FlightConverter.jar" %*
)	else (
	java -jar "%~p0FlightConverter.jar" %*
)
pause
