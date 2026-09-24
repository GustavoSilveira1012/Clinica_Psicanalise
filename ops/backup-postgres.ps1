[CmdletBinding()]
param(
    [string]$OutputDirectory = ".\backups\postgres",
    [int]$RetentionDays = 30,
    [switch]$PruneExpired
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot 'postgres-common.ps1')

foreach ($name in @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variável obrigatória ausente: $name"
    }
}

if ($RetentionDays -lt 1) {
    throw "RetentionDays deve ser maior que zero."
}

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump não encontrado no PATH. Instale o cliente PostgreSQL antes de executar o backup."
}

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
$databaseTarget = Get-PostgresTarget -DatabaseUrl $env:DATABASE_URL
New-Item -ItemType Directory -Path $resolvedOutput -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupPath = Join-Path $resolvedOutput "psicogest-$timestamp.dump"
$originalPassword = $env:PGPASSWORD

try {
    $env:PGPASSWORD = $env:DATABASE_PASSWORD
    & pg_dump `
        --format=custom `
        --file=$backupPath `
        --no-owner `
        --no-privileges `
        --dbname=$databaseTarget.ConnectionString `
        --username=$env:DATABASE_USERNAME

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $backupPath)) {
        throw "pg_dump não produziu um backup válido."
    }

    $hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $manifestPath = "$backupPath.sha256"
    Set-Content -LiteralPath $manifestPath -Value "$hash *$(Split-Path -Leaf $backupPath)" -Encoding ascii

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    if ($PruneExpired) {
    Get-ChildItem -LiteralPath $resolvedOutput -Filter "psicogest-*.dump" -File |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        Remove-Item -Force
    Get-ChildItem -LiteralPath $resolvedOutput -Filter "psicogest-*.dump.sha256" -File |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        Remove-Item -Force
    }

    Write-Output "Backup criado: $backupPath"
    Write-Output "SHA-256: $hash"
}
finally {
    if ($null -eq $originalPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:PGPASSWORD = $originalPassword
    }
}
