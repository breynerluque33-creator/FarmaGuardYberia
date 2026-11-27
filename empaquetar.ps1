$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

Write-Host '== Compilando, probando y generando el instalador con jpackage ==' -ForegroundColor Cyan
& .\mvnw.cmd -B -ntp -Pexe clean package
if ($LASTEXITCODE -ne 0) {
    throw "La compilación falló (código $LASTEXITCODE)."
}

$instalador = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'target\dist') -Filter 'FarmaGuard-*.exe' |
    Select-Object -First 1
if (-not $instalador) {
    throw 'No se encontró el instalador en target\dist'
}

$escritorio = [Environment]::GetFolderPath('Desktop')
Copy-Item -LiteralPath $instalador.FullName -Destination $escritorio -Force

Write-Host ("Listo: {0}\{1} ({2:N0} MB con Java incluido)" -f $escritorio, $instalador.Name, ($instalador.Length / 1MB)) -ForegroundColor Green
