@echo off
echo Preparazione dei file per GitHub...
git add .

echo.
echo Creazione del salvataggio automatico...
git commit -m "Aggiornamento automatico del codice"

echo.
echo Caricamento su GitHub in corso...
git push origin master

echo.
echo Caricamento completato!
pause