# HU 50: envía un aviso de prueba a los teléfonos registrados del usuario.
# Sirve para comprobar la recepción con la app abierta, en segundo plano y cerrada.
#
# Uso:
#   .\scripts\probar-notificaciones.ps1
#   .\scripts\probar-notificaciones.ps1 -Tipo campaign_reminder -CampanaId 3
#
# Antes de ejecutarlo, inicia sesión en la app para que el teléfono registre su token.
param(
    [string]$Api = "http://localhost:8090/api",
    [string]$Correo = "demo@adbrand.ai",
    [string]$Contrasena = "Demo1234",
    [ValidateSet("system_alert", "campaign_reminder", "subscription_reminder")]
    [string]$Tipo = "system_alert",
    [long]$CampanaId = 0
)

$ErrorActionPreference = "Stop"

$sesion = Invoke-RestMethod -Method Post -Uri "$Api/auth/login" `
    -ContentType "application/json" `
    -Body (@{ correo = $Correo; contrasena = $Contrasena } | ConvertTo-Json)

$cabeceras = @{ Authorization = "Bearer $($sesion.token)" }
$cuerpo = @{ tipo = $Tipo }
if ($CampanaId -gt 0) {
    $cuerpo.campanaId = $CampanaId
}

$respuesta = Invoke-RestMethod -Method Post -Uri "$Api/notificaciones/prueba" `
    -Headers $cabeceras -ContentType "application/json" `
    -Body ($cuerpo | ConvertTo-Json)

Write-Host ""
Write-Host "Aviso '$Tipo' enviado a $($respuesta.enviados) dispositivo(s)." -ForegroundColor Green

if ($Tipo -eq "system_alert") {
    Write-Host ""
    Write-Host "Prueba los tres estados:" -ForegroundColor Cyan
    Write-Host "  1. App abierta:    verás el banner arriba y la notificación local."
    Write-Host "  2. App en segundo plano: pulsa Inicio y repite el script."
    Write-Host "  3. App cerrada:    adb shell am force-stop com.adbrand y repite el script."
}

if ($respuesta.enviados -eq 0) {
    Write-Host ""
    Write-Host "No hay teléfonos registrados. Abre la app e inicia sesión primero." -ForegroundColor Yellow
}
