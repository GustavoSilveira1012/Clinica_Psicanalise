[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedDatabaseName,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot 'postgres-common.ps1')

foreach ($name in @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variável obrigatória ausente: $name"
    }
}

if (-not $Force) {
    throw "Restore é destrutivo. Informe -Force após validar ambiente, backup e janela de manutenção."
}

$resolvedBackup = [System.IO.Path]::GetFullPath($BackupPath)
$databaseTarget = Get-PostgresTarget -DatabaseUrl $env:DATABASE_URL
if ($databaseTarget.DatabaseName -cne $ExpectedDatabaseName) {
    throw 'Nome do banco de destino não corresponde a ExpectedDatabaseName. Restore cancelado.'
}
if (-not (Test-Path -LiteralPath $resolvedBackup -PathType Leaf)) {
    throw "Backup não encontrado: $resolvedBackup"
}

if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    throw "pg_restore não encontrado no PATH. Instale o cliente PostgreSQL antes de executar o restore."
}

$checksumPath = "$resolvedBackup.sha256"
if (-not (Test-Path -LiteralPath $checksumPath -PathType Leaf)) {
    throw 'Manifesto SHA-256 obrigatório. Restore cancelado.'
}
if (Test-Path -LiteralPath $checksumPath -PathType Leaf) {
    $expected = (Get-Content -LiteralPath $checksumPath -Raw).Trim().Split(" ")[0].ToLowerInvariant()
    $actual = (Get-FileHash -LiteralPath $resolvedBackup -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($expected -ne $actual) {
        throw "Checksum do backup não confere. Restore cancelado."
    }
}

$originalPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $env:DATABASE_PASSWORD
    if ($PSCmdlet.ShouldProcess($databaseTarget.SafeDescription, "Substituir dados pelo backup $resolvedBackup")) {
        & pg_restore `
            --exit-on-error `
            --clean `
            --if-exists `
            --single-transaction `
            --no-owner `
            --no-privileges `
            --dbname=$databaseTarget.ConnectionString `
            --username=$env:DATABASE_USERNAME `
            $resolvedBackup

        if ($LASTEXITCODE -ne 0) {
            throw "pg_restore falhou. Consulte a saída do cliente PostgreSQL e execute o smoke test de readiness."
        }
    }
}
finally {
    if ($null -eq $originalPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:PGPASSWORD = $originalPassword
    }
}
