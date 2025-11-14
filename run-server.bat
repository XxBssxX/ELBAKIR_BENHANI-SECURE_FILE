@echo off
if not exist bin (
    echo Erreur: Le projet n'a pas ete compile. Executez compile.bat d'abord.
    exit /b 1
)

if "%1"=="" (
    echo Demarrage du serveur sur le port 8888 (par defaut)...
    java -cp bin com.securefiletransfer.SecureFileServer
) else (
    echo Demarrage du serveur sur le port %1...
    java -cp bin com.securefiletransfer.SecureFileServer %1
)

