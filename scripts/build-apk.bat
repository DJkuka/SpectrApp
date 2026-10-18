@echo off
setlocal
cd /d "%~dp0\.."
where gradle >nul 2>nul
if errorlevel 1 (
  echo ERROR: Gradle is not installed or is not on PATH.
  echo Open this folder in Android Studio and run the Gradle task :android:assembleDebug,
  echo or install Gradle 8.13 and add it to PATH.
  exit /b 2
)
call scripts\test-core.bat
if errorlevel 1 exit /b 1
call gradle :android:assembleDebug
if errorlevel 1 exit /b 1
echo.
echo APK output: android\build\outputs\apk\debug\android-debug.apk
endlocal
