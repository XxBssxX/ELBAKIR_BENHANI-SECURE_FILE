@echo off
echo Compilation du projet Secure File Transfer...
if not exist bin mkdir bin
javac -d bin src/com/securefiletransfer/*.java
if %errorlevel% == 0 (
    echo Compilation reussie!
    echo Les fichiers compiles sont dans le dossier bin/
) else (
    echo Erreur lors de la compilation.
    exit /b 1
)

