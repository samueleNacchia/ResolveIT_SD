@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\..\.."

:: --- Risoluzione percorso completo cartella OpenJML ---
set "OPENJML_DIR="
for /f "delims=" %%p in ('where openjml.bat 2^>nul') do (
    if not defined OPENJML_DIR set "OPENJML_DIR=%%~dpp"
)
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

mkdir jml\statica 2>nul

echo [SETUP] Pulizia e compilazione classi compatibili Java 21...
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

echo [1/3] Analisi statica ESC: CategoriaService / CategoriaImpl...
call "%OPENJML_CMD%" --esc --timeout 30 --progress --specs-path src/main/java -cp "%CP%" ^
 src\main\java\it\unisa\resolveIt\categoria\service\CategoriaService.java ^
 src\main\java\it\unisa\resolveIt\categoria\service\CategoriaImpl.java > jml\statica\categoria.txt 2>&1

echo [2/3] Analisi statica ESC: AccountService / AccountImpl...
call "%OPENJML_CMD%" --esc --timeout 30 --progress --specs-path src/main/java -cp "%CP%" ^
 src\main\java\it\unisa\resolveIt\account\service\AccountService.java ^
 src\main\java\it\unisa\resolveIt\account\service\AccountImpl.java > jml\statica\account.txt 2>&1

echo [3/3] Analisi statica ESC: TicketService / TicketImpl...
call "%OPENJML_CMD%" --esc --timeout 30 --progress --specs-path src/main/java -cp "%CP%" ^
 src\main\java\it\unisa\resolveIt\ticket\service\TicketService.java ^
 src\main\java\it\unisa\resolveIt\ticket\service\TicketImpl.java > jml\statica\ticket.txt 2>&1

echo.
echo Analisi statica completata! Controlla jml\statica\ (categoria.txt, account.txt, ticket.txt)
pause