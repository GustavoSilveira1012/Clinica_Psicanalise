[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedDatabaseName,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedHost,
    [int]$ExpectedPort = 5432,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot 'postgres-common.ps1')

foreach ($name in @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD", "BACKUP_AGE_IDENTITY_FILE")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variável obrigatória ausente: $name"
    }
}

if (-not $Force) {
    throw "Restore é destrutivo. Informe -Force após validar ambiente, backup e janela de manutenção."
}

$resolvedBackup = [System.IO.Path]::GetFullPath($BackupPath)
$databaseTarget = Get-PostgresTarget -DatabaseUrl $env:DATABASE_URL
Assert-PostgresTargetMatches `
    -Target $databaseTarget `
    -ExpectedDatabaseName $ExpectedDatabaseName `
    -ExpectedHost $ExpectedHost `
    -ExpectedPort $ExpectedPort
if ([System.IO.Path]::GetExtension($resolvedBackup) -ne '.age' -or $resolvedBackup -notmatch '\.dump\.age$') {
    throw 'O restore aceita somente arquivos .dump.age criptografados.'
}
if (-not (Test-Path -LiteralPath $resolvedBackup -PathType Leaf)) {
    throw "Backup não encontrado: $resolvedBackup"
}

if (-not (Test-Path -LiteralPath $env:BACKUP_AGE_IDENTITY_FILE -PathType Leaf)) {
    throw 'BACKUP_AGE_IDENTITY_FILE deve apontar para uma chave privada age legível.'
}

$pgRestore = Get-Command pg_restore -CommandType Application -ErrorAction SilentlyContinue
if (-not $pgRestore) {
    throw "pg_restore não encontrado no PATH. Instale o cliente PostgreSQL antes de executar o restore."
}
$age = Get-Command age -CommandType Application -ErrorAction SilentlyContinue
if (-not $age) {
    throw 'age não encontrado no PATH. A descriptografia em fluxo está desabilitada.'
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

if ($PSCmdlet.ShouldProcess($databaseTarget.SafeDescription, "Descriptografar e substituir dados pelo backup $resolvedBackup")) {
    Invoke-BinaryPipe `
        -SourceExecutable $age.Source `
        -SourceArguments @(
            '--decrypt',
            '--identity',
            $env:BACKUP_AGE_IDENTITY_FILE,
            $resolvedBackup
        ) `
        -DestinationExecutable $pgRestore.Source `
        -DestinationArguments @(
            '--exit-on-error',
            '--clean',
            '--if-exists',
            '--single-transaction',
            '--no-owner',
            '--no-privileges',
            "--dbname=$($databaseTarget.ConnectionString)",
            "--username=$env:DATABASE_USERNAME",
            '-'
        ) `
        -DestinationEnvironment @{ PGPASSWORD = $env:DATABASE_PASSWORD }
}
