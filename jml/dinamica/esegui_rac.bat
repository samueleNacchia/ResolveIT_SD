@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\..\.."

:: --- Risoluzione percorso completo cartella OpenJML ---
set "OPENJML_DIR="
for %%i in (openjml.bat) do set "OPENJML_DIR=%%~dp$PATH:i"
if not defined OPENJML_DIR (
    if defined OPENJML_HOME (
        set "OPENJML_DIR=%OPENJML_HOME%\"
    ) else if exist "C:\tools\OpenJML\openjml.bat" (
        set "OPENJML_DIR=C:\tools\OpenJML\"
    ) else if exist "%USERPROFILE%\OpenJML\openjml.bat" (
        set "OPENJML_DIR=%USERPROFILE%\OpenJML\"
    ) else (
        echo [ERRORE] openjml.bat non trovato nel PATH o in C:\tools\OpenJML.
        echo Imposta la variabile OPENJML_HOME o aggiungi OpenJML al PATH.
        pause
        exit /b 1
    )
)
set "OPENJML_CMD=%OPENJML_DIR%openjml.bat"
set "OPENJML_JAVA_CMD=%OPENJML_DIR%openjml-java.bat"

mkdir jml\dinamica 2>nul
if exist "target\jml-rac" rmdir /s /q "target\jml-rac"
mkdir target\jml-rac 2>nul

echo [1/4] Pulizia e compilazione classi progetto (Java 21)...
call mvnw.cmd clean compile -Djava.version=21 -Dmaven.compiler.release=21 -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERRORE] Compilazione Maven fallita.
    pause
    exit /b 1
)

if not exist "target\jml-lib\*persistence*.jar" (
    echo [SETUP] Estrazione dipendenze Maven...
    if exist "target\jml-lib" rmdir /s /q "target\jml-lib"
    call mvnw.cmd dependency:copy-dependencies -DincludeScope=compile -DoutputDirectory=target/jml-lib
)

set "CP=target\classes"
for %%f in (target\jml-lib\*.jar) do set "CP=!CP!;%%f"

echo [2/4] Strumentazione RAC delle interfacce Service (contratti JML)...
call "%OPENJML_CMD%" --rac --specs-path src/main/java -cp "%CP%" -d target\jml-rac ^
 src\main\java\it\unisa\resolveIt\categoria\service\CategoriaService.java ^
 src\main\java\it\unisa\resolveIt\account\service\AccountService.java ^
 src\main\java\it\unisa\resolveIt\ticket\service\TicketService.java > jml\dinamica\rac_compile.txt 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [ERRORE] Strumentazione RAC fallita. Controlla jml\dinamica\rac_compile.txt
    type jml\dinamica\rac_compile.txt
    pause
    exit /b 1
)

echo [3/4] Compilazione driver TestRacService (Java 21)...
javac --release 21 -cp "target\jml-rac;%CP%" -d target\jml-rac jml\dinamica\TestRacService.java > jml\dinamica\driver_compile.txt 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [ERRORE] Compilazione TestRacService fallita. Controlla jml\dinamica\driver_compile.txt
    type jml\dinamica\driver_compile.txt
    pause
    exit /b 1
)

echo [4/4] Esecuzione analisi dinamica RAC...
call "%OPENJML_JAVA_CMD%" -cp "target\jml-rac;%CP%" jml.dinamica.TestRacService > jml\dinamica\rac_output.txt 2>&1

type jml\dinamica\rac_output.txt
echo.
echo Output salvato in jml\dinamica\rac_output.txt
pause