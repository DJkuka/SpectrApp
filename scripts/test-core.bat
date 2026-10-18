@echo off
setlocal
set ROOT=%~dp0..
set OUT=%ROOT%build\core-smoke-classes
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"
javac -encoding UTF-8 -d "%OUT%" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\Element.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\CardType.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\CardDefinition.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\CreatureState.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\PlayerState.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\CardCatalog.java" ^
  "%ROOT%core\src\main\java\com\spectromancer\port\BattleEngine.java" ^
  "%ROOT%tests\BattleEngineSmokeTest.java"
if errorlevel 1 exit /b 1
java -cp "%OUT%" com.spectromancer.port.BattleEngineSmokeTest
endlocal
