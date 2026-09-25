$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'postgres-common.ps1')

function Assert-Throws {
    param([scriptblock]$Action, [string]$Name)
    $thrown = $false
    try { & $Action } catch { $thrown = $true }
    if (-not $thrown) { throw "Teste falhou: $Name deveria ser rejeitado." }
}

$validTarget = Get-PostgresTarget 'jdbc:postgresql://db-clone.example.invalid:5432/psicogest_restore_drill'
Assert-PostgresTargetMatches `
    -Target $validTarget `
    -ExpectedDatabaseName 'psicogest_restore_drill' `
    -ExpectedHost 'DB-CLONE.example.invalid.' `
    -ExpectedPort 5432

Assert-Throws {
    Assert-PostgresTargetMatches -Target $validTarget -ExpectedDatabaseName 'production' `
        -ExpectedHost 'db-clone.example.invalid' -ExpectedPort 5432
} 'nome de banco diferente'
Assert-Throws {
    Assert-PostgresTargetMatches -Target $validTarget -ExpectedDatabaseName 'psicogest_restore_drill' `
        -ExpectedHost 'production.example.invalid' -ExpectedPort 5432
} 'host diferente'
Assert-Throws {
    Assert-PostgresTargetMatches -Target $validTarget -ExpectedDatabaseName 'psicogest_restore_drill' `
        -ExpectedHost 'db-clone.example.invalid' -ExpectedPort 5433
} 'porta diferente'
Assert-Throws {
    Get-PostgresTarget 'jdbc:postgresql://user:secret@db.example.invalid/production'
} 'credencial embutida na URL'

$temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) ('psicogest-pipe-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null
try {
    $outputPath = Join-Path $temporaryDirectory 'stream.bin'
    $encodedPath = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($outputPath))
    $powershell = (Get-Command pwsh -CommandType Application).Source
    $sourceCode = '[byte[]]$bytes = 0..255; [Console]::OpenStandardOutput().Write($bytes, 0, $bytes.Length)'
    $destinationCode = '$path=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(' +
        "'" + $encodedPath + "'" + ')); $stream=[Console]::OpenStandardInput(); ' +
        '$bytes=[Collections.Generic.List[byte]]::new(); while (($value=$stream.ReadByte()) -ne -1) { $bytes.Add([byte]$value) }; ' +
        '[IO.File]::WriteAllBytes($path, $bytes.ToArray())'

    Invoke-BinaryPipe `
        -SourceExecutable $powershell `
        -SourceArguments @('-NoProfile', '-Command', $sourceCode) `
        -DestinationExecutable $powershell `
        -DestinationArguments @('-NoProfile', '-Command', $destinationCode)

    $actualBytes = [IO.File]::ReadAllBytes($outputPath)
    if ($actualBytes.Length -ne 256) { throw 'Teste falhou: tamanho do fluxo binário alterado.' }
    for ($index = 0; $index -lt 256; $index++) {
        if ($actualBytes[$index] -ne $index) { throw "Teste falhou: byte alterado no índice $index." }
    }

    $backupDirectory = Join-Path $temporaryDirectory 'backup-prune'
    New-Item -ItemType Directory -Path $backupDirectory | Out-Null
    $expiredBackupPath = Join-Path $backupDirectory 'psicogest-expired.dump.age'
    $expiredManifestPath = "$expiredBackupPath.sha256"
    Set-Content -LiteralPath $expiredBackupPath -Value 'synthetic-ciphertext'
    Set-Content -LiteralPath $expiredManifestPath -Value 'synthetic-checksum'
    $oldTime = (Get-Date).AddDays(-31)
    (Get-Item -LiteralPath $expiredBackupPath).LastWriteTime = $oldTime
    (Get-Item -LiteralPath $expiredManifestPath).LastWriteTime = $oldTime

    Remove-ExpiredPostgresBackups -Directory $backupDirectory -Before (Get-Date) -WhatIf
    if (-not (Test-Path -LiteralPath $expiredBackupPath) -or -not (Test-Path -LiteralPath $expiredManifestPath)) {
        throw 'Teste falhou: -WhatIf removeu backup ou manifesto.'
    }

    Remove-ExpiredPostgresBackups -Directory $backupDirectory -Before (Get-Date) -Confirm:$false
    if ((Test-Path -LiteralPath $expiredBackupPath) -or (Test-Path -LiteralPath $expiredManifestPath)) {
        throw 'Teste falhou: backup expirado e manifesto associado deveriam ser removidos juntos.'
    }
} finally {
    $resolvedDirectory = [IO.Path]::GetFullPath($temporaryDirectory)
    $resolvedTempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if ($resolvedDirectory.StartsWith($resolvedTempRoot, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolvedDirectory) -like 'psicogest-pipe-*') {
        Remove-Item -LiteralPath $resolvedDirectory -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PostgreSQL safety checks passed. No database connection was opened.'
