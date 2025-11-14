@echo off
if not exist bin (
    echo Erreur: Le projet n'a pas ete compile. Executez compile.bat d'abord.
    exit /b 1
)

if "%5"=="" (
    echo Usage: run-client.bat ^<serverAddress^> ^<port^> ^<login^> ^<password^> ^<filePath^>
    echo Exemple: run-client.bat localhost 8888 admin admin123 C:\fichier.txt
    exit /b 1
)

java -cp bin com.securefiletransfer.SecureFileClient %1 %2 %3 %4 %5

