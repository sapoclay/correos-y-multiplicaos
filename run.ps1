#!/usr/bin/env pwsh
<#!
.SYNOPSIS
  Script de lanzamiento para Windows (PowerShell) con mayor robustez.
.DESCRIPTION
  - Prioriza el uber-jar.
  - Si falta, construye con Maven.
  - Si no aparece tras la construcción, usa classpath dinámico.
  - Ajusta encoding a UTF-8.
.NOTES
  Requiere Maven en PATH y Java 21 instalado.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Info($msg) { Write-Host "[run.ps1] $msg" }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarPath   = Join-Path $ScriptDir 'target/gestor-correo-1.0-SNAPSHOT.jar'

# Detectar Java
$javaVersion = & java -version 2>&1 | Select-Object -First 1
if (-not $javaVersion) { Write-Error 'Java no encontrado en PATH.' }
Write-Info "Java detectado: $javaVersion"

# Ajustes específicos por SO
if ($IsLinux) {
  Write-Info 'Entorno Linux detectado: ajustando variables (GTK/IME).'
  # Variables problemáticas de GTK/snap
  Remove-Item Env:GTK_PATH -ErrorAction SilentlyContinue
  Remove-Item Env:LD_PRELOAD -ErrorAction SilentlyContinue
  Remove-Item Env:LD_LIBRARY_PATH -ErrorAction SilentlyContinue
  $Env:GTK_MODULES = ""
  # Locale e IME para acentos/teclas muertas
  if (-not $Env:LANG)   { $Env:LANG = 'es_ES.UTF-8' }
  if (-not $Env:LC_ALL) { $Env:LC_ALL = 'es_ES.UTF-8' }
  if (-not $Env:GTK_IM_MODULE) { $Env:GTK_IM_MODULE = 'ibus' }
  if (-not $Env:QT_IM_MODULE)  { $Env:QT_IM_MODULE  = 'ibus' }
  if (-not $Env:XMODIFIERS)    { $Env:XMODIFIERS    = '@im=ibus' }
  if (-not $Env:GDK_BACKEND)   { $Env:GDK_BACKEND   = 'x11' }
} elseif ($IsWindows) {
  Write-Info 'Entorno Windows detectado.'
}

# Funcion para construir uber-jar
function Build-Jar {
  Write-Info 'Construyendo JAR (mvn package -DskipTests)...'
  & mvn -q -DskipTests package | Out-Null
}

if (Test-Path $JarPath) {
  $useJar = $true
} else {
  Write-Info 'JAR no encontrado. Intentando construir.'
  Build-Jar
  if (Test-Path $JarPath) {
    Write-Info 'JAR construido correctamente.'
    $useJar = $true
  } else {
    Write-Info 'Fallo al construir JAR. Usando fallback por classpath.'
    & mvn -q -DskipTests compile | Out-Null
    $depList = (& mvn -q -DskipTests dependency:build-classpath -Dmdep.outputFile=/dev/stdout | Select-Object -Last 1)
    $sep = [IO.Path]::PathSeparator
    $classesPath = Join-Path $ScriptDir 'target/classes'
    $classPath = "$classesPath$sep$depList"
    $useJar = $false
  }
}

$javaOpts = @(
  '-Dfile.encoding=UTF-8',
  '-Dsun.jnu.encoding=UTF-8',
  '-Dswing.aatext=true'
)

if ($useJar) {
  Write-Info "Ejecutando JAR: $JarPath"
  & java $javaOpts -jar $JarPath
} else {
  Write-Info "Ejecutando con classpath dinámico: $classPath"
  & java $javaOpts -cp $classPath com.gestorcorreo.Main
}
