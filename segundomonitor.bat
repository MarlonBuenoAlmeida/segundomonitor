@echo off
title Transmissao e Extensao de Tela para Tablet

echo Alterando exibicao do Windows para modo ESTENDER...
displayswitch.exe /extend

timeout /t 3 /nobreak >nul

echo Redirecionando portas ADB (9090)...
adb forward tcp:9090 tcp:9090

timeout /t 2 /nobreak >nul

echo Navegando ate a pasta do projeto...
cd /d "D:\projetos\segundomonitor"

echo Iniciando transmissao de tela...
start /b pythonw screen_sender.py

exit