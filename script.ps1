# script.ps1
$clientCount = 5  # Número de clientes que deseja iniciar

for ($i = 1; $i -le $clientCount; $i++) {
    Write-Host "Iniciando cliente $i..."
    Start-Process "docker-compose" -ArgumentList "run", "--rm", "cliente"
}
