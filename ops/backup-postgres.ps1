[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [string]$OutputDirectory = ".\backups\postgres",
    [int]$RetentionDays = 30,
    [switch]$PruneExpired
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot 'postgres-common.ps1')

if ($PSVersionTable.PSVersion -lt [version]'7.0') {
    throw 'PowerShell 7 ou superior é obrigatório para transmitir o dump binário sem arquivo plaintext temporário.'
}

foreach ($name in @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD", "BACKUP_AGE_RECIPIENT")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variável obrigatória ausente: $name"
    }
}

if ($env:BACKUP_AGE_RECIPIENT -notmatch '^age1[023456789acdefghjklmnpqrstuvwxyz]{20,80}$') {
    throw 'BACKUP_AGE_RECIPIENT deve ser um destinatário age X25519 público válido.'
}

if ($RetentionDays -lt 1) {
    throw "RetentionDays deve ser maior que zero."
}

if (-not (Get-Command pg_dump -CommandType Application -ErrorAction SilentlyContinue)) {
    throw "pg_dump não encontrado no PATH. Instale o cliente PostgreSQL antes de executar o backup."
}
if (-not (Get-Command age -CommandType Application -ErrorAction SilentlyContinue)) {
    throw "age não encontrado no PATH. O backup plaintext está desabilitado."
}

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
$databaseTarget = Get-PostgresTarget -DatabaseUrl $env:DATABASE_URL
New-Item -ItemType Directory -Path $resolvedOutput -Force | Out-Null

$timestamp = (Get-Date -Format "yyyyMMdd-HHmmss") + "-" + [Guid]::NewGuid().ToString("N").Substring(0, 8)
$backupPath = Join-Path $resolvedOutput "psicogest-$timestamp.dump.age"
$manifestPath = "$backupPath.sha256"
$pgDump = Get-Command pg_dump -CommandType Application
$age = Get-Command age -CommandType Application
$backupCompleted = $false

try {
    Invoke-BinaryPipe `
        -SourceExecutable $pgDump.Source `
        -SourceArguments @(
            "--format=custom",
            "--file=-",
            "--no-owner",
            "--no-privileges",
            "--dbname=$($databaseTarget.ConnectionString)",
            "--username=$env:DATABASE_USERNAME"
        ) `
        -SourceEnvironment @{ PGPASSWORD = $env:DATABASE_PASSWORD } `
        -DestinationExecutable $age.Source `
        -DestinationArguments @(
            "--recipient",
            $env:BACKUP_AGE_RECIPIENT,
            "--output",
            $backupPath
        )

    if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf) -or (Get-Item -LiteralPath $backupPath).Length -eq 0) {
        throw "pg_dump/age não produziu um backup criptografado válido."
    }

    $hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath $manifestPath -Value "$hash *$(Split-Path -Leaf $backupPath)" -Encoding ascii
    $backupCompleted = $true

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    if ($PruneExpired) {
        Remove-ExpiredPostgresBackups -Directory $resolvedOutput -Before $cutoff
    }

    Write-Output "Backup age criptografado criado: $backupPath"
    Write-Output "SHA-256: $hash"
}
finally {
    if (-not $backupCompleted) {
        Remove-Item -LiteralPath $backupPath -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $manifestPath -Force -ErrorAction SilentlyContinue
    }
}
